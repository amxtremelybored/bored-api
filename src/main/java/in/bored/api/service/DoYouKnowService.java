package in.bored.api.service;

import in.bored.api.model.*;
import in.bored.api.repo.DoYouKnowCategoryRepository;
import in.bored.api.repo.DoYouKnowContentRepository;
import in.bored.api.repo.UserDoYouKnowViewRepository;
import in.bored.api.repo.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DoYouKnowService {

    private static final Logger logger = LoggerFactory.getLogger(DoYouKnowService.class);

    private final DoYouKnowCategoryRepository categoryRepository;
    private final DoYouKnowContentRepository contentRepository;
    private final UserDoYouKnowViewRepository userViewRepository;
    private final UserProfileRepository userProfileRepository;
    private final GeminiService geminiService;

    public DoYouKnowService(DoYouKnowCategoryRepository categoryRepository,
            DoYouKnowContentRepository contentRepository,
            UserDoYouKnowViewRepository userViewRepository,
            UserProfileRepository userProfileRepository,
            GeminiService geminiService) {
        this.categoryRepository = categoryRepository;
        this.contentRepository = contentRepository;
        this.userViewRepository = userViewRepository;
        this.userProfileRepository = userProfileRepository;
        this.geminiService = geminiService;
    }

    public List<DoYouKnowContent> getNextDoYouKnowForCurrentUser(int count) {
        UserProfile user = getCurrentUserProfile();
        if (user == null) {
            logger.warn("No authenticated user found for do you know fetch.");
            return List.of();
        }

        // 1. Try to find unseen facts in DB
        // 1. Try to find unseen facts in DB
        List<DoYouKnowContent> existing = contentRepository.findRandomUnseen(user.getId(), count);
        if (existing.size() >= count) {
            return existing;
        }

        // 2. If not enough, generate new facts via Gemini
        logger.info("Not enough unseen facts for user {} (found {}), generating more...", user.getId(),
                existing.size());
        generateAndSaveDoYouKnow(10); // Deduplicates internally

        // 3. Try fetching again
        // 3. Try fetching again
        List<DoYouKnowContent> finalResult = contentRepository.findRandomUnseen(user.getId(), count);

        // 4. Mark all as served/viewed to prevent duplicates
        for (DoYouKnowContent dc : finalResult) {
            if (!userViewRepository.existsByUserProfileIdAndDoYouKnowContentId(user.getId(), dc.getId())) {
                try {
                    UserDoYouKnowView view = new UserDoYouKnowView(user.getId(), dc.getId(), null);
                    userViewRepository.save(view);
                } catch (Exception e) {
                    logger.warn("Could not save fact view: {}", e.getMessage());
                }
            }
        }

        return finalResult;
    }

    public void markDoYouKnowAsViewed(Long contentId, Boolean isLiked) {
        UserProfile user = getCurrentUserProfile();
        if (user == null)
            return;

        try {
            Optional<UserDoYouKnowView> existingView = userViewRepository
                    .findByUserProfileIdAndDoYouKnowContentId(user.getId(), contentId);

            if (existingView.isPresent()) {
                UserDoYouKnowView view = existingView.get();
                if (isLiked != null) {
                    view.setIsLiked(isLiked);
                    userViewRepository.save(view);
                }
            } else {
                UserDoYouKnowView view = new UserDoYouKnowView(user.getId(), contentId, isLiked);
                userViewRepository.save(view);
            }
        } catch (Exception e) {
            logger.warn("Failed to mark fact {} as viewed for user {}: {}", contentId, user.getId(), e.getMessage());
        }
    }

    private void generateAndSaveDoYouKnow(int count) {
        List<String> newItems = geminiService.generateDoYouKnow(count);
        if (newItems.isEmpty())
            return;

        // Ensure default category exists
        DoYouKnowCategory category = categoryRepository.findByName("General Facts")
                .orElseGet(() -> categoryRepository
                        .save(new DoYouKnowCategory("General Facts", "Interesting general knowledge facts")));

        for (String fact : newItems) {
            java.util.Optional<DoYouKnowContent> existing = contentRepository.findByFact(fact);
            if (existing.isEmpty()) {
                DoYouKnowContent content = new DoYouKnowContent(category.getId(), fact, "Gemini");
                contentRepository.save(content);
            }
        }
    }

    private UserProfile getCurrentUserProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return null;
        String uid = auth.getName();
        return userProfileRepository.findByUidAndStatusNot(uid, ProfileStatus.DELETED).orElse(null);
    }
}
