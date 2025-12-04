package in.bored.api.service;

import in.bored.api.model.HealthWellnessCategory;
import in.bored.api.model.HealthWellnessContent;
import in.bored.api.model.UserHealthWellnessView;
import in.bored.api.repo.HealthWellnessCategoryRepository;
import in.bored.api.repo.HealthWellnessContentRepository;
import in.bored.api.repo.UserHealthWellnessViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class HealthWellnessService {

    private static final Logger logger = LoggerFactory.getLogger(HealthWellnessService.class);

    private final HealthWellnessContentRepository contentRepository;
    private final HealthWellnessCategoryRepository categoryRepository;
    private final UserHealthWellnessViewRepository viewRepository;
    private final GeminiService geminiService;

    public HealthWellnessService(HealthWellnessContentRepository contentRepository,
            HealthWellnessCategoryRepository categoryRepository,
            UserHealthWellnessViewRepository viewRepository,
            GeminiService geminiService) {
        this.contentRepository = contentRepository;
        this.categoryRepository = categoryRepository;
        this.viewRepository = viewRepository;
        this.geminiService = geminiService;
    }

    @Transactional
    public HealthWellnessContent getNextTip(Long userProfileId, String topic) {
        Optional<HealthWellnessCategory> categoryOpt = categoryRepository.findByName(topic);
        if (categoryOpt.isEmpty()) {
            logger.warn("Category not found: {}", topic);
            return null;
        }

        HealthWellnessCategory category = categoryOpt.get();

        // 1. Try to find unseen content in DB
        Optional<HealthWellnessContent> existing = contentRepository.findRandomUnseen(userProfileId, category.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. If no content, generate via Gemini
        logger.info("No existing health/wellness content for user {} in category {}. Generating...", userProfileId,
                topic);
        List<String> newTips = geminiService.generateHealthWellnessTip(topic, 5);

        if (newTips.isEmpty()) {
            return null;
        }

        // 3. Save new content
        for (String tipText : newTips) {
            HealthWellnessContent content = new HealthWellnessContent();
            content.setCategory(category);
            content.setTip(tipText);
            content.setSource("Gemini");
            contentRepository.save(content);
        }

        // 4. Return one of the new items (the first one)
        // We need to fetch it again or just use the first saved one.
        // Let's just return the first one we created.
        HealthWellnessContent first = new HealthWellnessContent();
        first.setCategory(category);
        first.setTip(newTips.get(0));
        first.setSource("Gemini");
        // We need the ID to be set, so we should probably fetch the one we just saved
        // or save and return.
        // The loop above saves them. Let's find the one we just saved.
        // Actually, let's just return the first one from the loop but we need the ID
        // for tracking view.
        // So let's query again.
        return contentRepository.findRandomUnseen(userProfileId, category.getId()).orElse(null);
    }

    @Transactional
    public void markAsViewed(Long userProfileId, Long contentId, Boolean isLiked) {
        try {
            UserHealthWellnessView view = new UserHealthWellnessView(userProfileId, contentId, isLiked);
            viewRepository.save(view);
        } catch (Exception e) {
            // Ignore duplicate views
            logger.debug("Already viewed: user={} content={}", userProfileId, contentId);
        }
    }
}
