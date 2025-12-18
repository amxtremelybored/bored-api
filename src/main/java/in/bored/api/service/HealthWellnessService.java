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
    public List<HealthWellnessContent> getNextTips(Long userProfileId, String topic, int count) {
        Optional<HealthWellnessCategory> categoryOpt = categoryRepository.findByName(topic);
        if (categoryOpt.isEmpty()) {
            logger.warn("Category not found: {}", topic);
            return List.of();
        }

        HealthWellnessCategory category = categoryOpt.get();

        // 1. Try to find unseen content in DB
        List<HealthWellnessContent> existing = contentRepository.findRandomUnseen(userProfileId, category.getId(),
                count);

        // 2. If not enough content, generate via Gemini
        if (existing.size() < count) {
            int needed = count - existing.size();
            logger.info(
                    "Not enough existing content (found {}), generating {} more via Gemini for user {} in category {}",
                    existing.size(), needed, userProfileId, topic);

            // Ask for a bit more to handle potential duplicates
            List<String> newTips = geminiService.generateHealthWellnessTip(topic, Math.max(5, needed));

            if (!newTips.isEmpty()) {
                // 3. Save new content
                for (String tipText : newTips) {
                    // Check for duplicates
                    Optional<HealthWellnessContent> existingTip = contentRepository.findByTip(tipText);
                    if (existingTip.isPresent()) {
                        // If it's a dry run, we might want to use it if we haven't seen it?
                        // But findRandomUnseen should catch it on next pass.
                        continue;
                    }
                    HealthWellnessContent content = new HealthWellnessContent();
                    content.setCategory(category);
                    content.setTip(tipText);
                    content.setSource("Gemini");
                    contentRepository.save(content);
                }

                // 4. Re-fetch to get the new ones (and any missed ones)
                existing = contentRepository.findRandomUnseen(userProfileId, category.getId(), count);
            }
        }

        // 5. Mark as viewed
        for (HealthWellnessContent content : existing) {
            markAsViewed(userProfileId, content.getId(), null);
        }

        return existing;
    }

    @Transactional
    public void markAsViewed(Long userProfileId, Long contentId, Boolean isLiked) {
        if (viewRepository.existsByUserProfileIdAndHealthWellnessContentId(userProfileId, contentId)) {
            return;
        }
        try {
            UserHealthWellnessView view = new UserHealthWellnessView(userProfileId, contentId, isLiked);
            viewRepository.save(view);
        } catch (Exception e) {
            // Ignore duplicate views
            logger.debug("Already viewed: user={} content={}", userProfileId, contentId);
        }
    }
}
