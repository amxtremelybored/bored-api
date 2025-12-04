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

    public DoYouKnowContent getNextDoYouKnowForCurrentUser() {
        UserProfile user = getCurrentUserProfile();
        if (user == null) {
            logger.warn("No authenticated user found for do you know fetch.");
            return null;
        }

        // 1. Try to find an unseen fact in DB
        Optional<DoYouKnowContent> existing = contentRepository.findRandomUnseen(user.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. If none, generate new facts via Gemini
        logger.info("No unseen facts for user {}, generating more...", user.getId());
        generateAndSaveDoYouKnow(5);

        // 3. Try fetching again
        return contentRepository.findRandomUnseen(user.getId()).orElse(null);
    }

    public void markDoYouKnowAsViewed(Long contentId, Boolean isLiked) {
        UserProfile user = getCurrentUserProfile();
        if (user == null)
            return;

        try {
            UserDoYouKnowView view = new UserDoYouKnowView(user.getId(), contentId, isLiked);
            userViewRepository.save(view);
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
            DoYouKnowContent content = new DoYouKnowContent(category.getId(), fact, "Gemini");
            contentRepository.save(content);
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
