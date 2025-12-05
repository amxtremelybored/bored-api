package in.bored.api;

import in.bored.api.dto.ContentItemResponse;
import in.bored.api.model.ContentCategory;
import in.bored.api.model.Topic;
import in.bored.api.model.TopicContent;
import in.bored.api.repo.ContentCategoryRepository;
import in.bored.api.repo.TopicContentRepository;
import in.bored.api.repo.TopicRepository;
import in.bored.api.service.GeminiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Standalone script to load content for all topics.
 * Usage: Run this main method. Ensure DB connection params are in
 * application.yml
 */
public class ContentLoaderScript {

    private static final Logger logger = LoggerFactory.getLogger(ContentLoaderScript.class);

    public static void main(String[] args) {
        System.out.println("🚀 Starting Content Loader Script...");

        SpringApplication app = new SpringApplication(BoredApiApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE); // Do not start web server
        app.setBannerMode(Banner.Mode.OFF);
        app.setAdditionalProfiles("local"); // Activate 'application-local.yml' automatically
        // Disable Flyway/Liquibase if needed, or let them run to ensure schema is up to
        // date.

        try (ConfigurableApplicationContext context = app.run(args)) {
            runLoader(context);
        } catch (Exception e) {
            logger.error("❌ Script failed", e);
        }

        System.out.println("✅ Content Loader Script Finished.");
        System.exit(0);
    }

    private static void runLoader(ConfigurableApplicationContext context) {
        ContentCategoryRepository categoryRepo = context.getBean(ContentCategoryRepository.class);
        TopicRepository topicRepo = context.getBean(TopicRepository.class);
        TopicContentRepository topicContentRepo = context.getBean(TopicContentRepository.class);
        GeminiService geminiService = context.getBean(GeminiService.class);

        logger.info("📦 Fetching categories...");
        List<ContentCategory> categories = categoryRepo.findAll();
        logger.info("Found {} categories.", categories.size());

        for (ContentCategory category : categories) {
            logger.info("📂 Processing Category: {}", category.getName());
            List<Topic> topics = topicRepo.findByCategory(category);

            for (Topic topic : topics) {
                logger.info("   👉 Topic: {} (ID: {})", topic.getName(), topic.getId());

                // User requested 50 items per topic.
                // We will fetch in 5 batches of 10 to ensure high quality and avoid timeouts.
                int totalTarget = 50;
                int batchSize = 10;
                int batches = totalTarget / batchSize;

                logger.info("      🤖 Generating {} facts (in {} batches)...", totalTarget, batches);

                for (int b = 1; b <= batches; b++) {
                    try {
                        logger.info("      🚀 [{} > {}] Batch {}/{}: Requesting {} items...", category.getName(),
                                topic.getName(), b, batches, batchSize);
                        List<ContentItemResponse> items = geminiService.generateContent(
                                topic.getName(),
                                category.getName(),
                                batchSize);

                        if (items.isEmpty()) {
                            logger.warn("      ⚠️ Batch {} empty for {}", b, topic.getName());
                            continue;
                        }

                        // Find current max index to append (fetch fresh each time to be safe)
                        Integer maxIndex = topicContentRepo.findMaxContentIndexByTopic(topic);
                        int nextIndex = (maxIndex == null) ? 0 : maxIndex + 1;

                        List<TopicContent> newContents = new ArrayList<>();
                        for (ContentItemResponse item : items) {
                            TopicContent tc = new TopicContent();
                            tc.setTopic(topic);
                            tc.setContent(item.getContent());
                            tc.setContentIndex(nextIndex++);
                            tc.setCreatedAt(OffsetDateTime.now());
                            newContents.add(tc);
                        }

                        topicContentRepo.saveAll(newContents);

                        // Log the loaded content for visibility
                        for (TopicContent tc : newContents) {
                            // Truncate if too long for clean logs, or show mostly full if short facts
                            String text = tc.getContent().replace("\n", " ");
                            if (text.length() > 80)
                                text = text.substring(0, 77) + "...";
                            logger.info("          🔹 {}", text);
                        }

                        logger.info("      ✅ Saved batch {} ({} items). Total so far: {}", b, newContents.size(),
                                (b * batchSize));

                        // Small delay between batches to be nice to the API
                        Thread.sleep(1500);

                    } catch (Exception e) {
                        logger.error("      ❌ Error in batch {} for topic {}: {}", b, topic.getName(), e.getMessage());
                    }
                }

                // Mark topic as loaded (after attempting all batches)
                if (!topic.isContentLoaded()) {
                    topic.setContentLoaded(true);
                    topic.setContentLoadedAt(OffsetDateTime.now());
                    topicRepo.save(topic);
                    logger.info("      🏁 Marked topic {} as loaded.", topic.getName());
                }
            }

            // Mark category as loaded
            category.setContentLoaded(true);
            category.setContentLoadedAt(OffsetDateTime.now());
            categoryRepo.save(category);
        }
    }
}
