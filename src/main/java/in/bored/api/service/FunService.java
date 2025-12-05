package in.bored.api.service;

import in.bored.api.model.*;
import in.bored.api.repo.FunCategoryRepository;
import in.bored.api.repo.FunContentRepository;
import in.bored.api.repo.UserFunViewRepository;
import in.bored.api.repo.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FunService {

    private static final Logger logger = LoggerFactory.getLogger(FunService.class);

    private final FunCategoryRepository categoryRepository;
    private final FunContentRepository contentRepository;
    private final UserFunViewRepository userViewRepository;
    private final UserProfileRepository userProfileRepository;
    private final GeminiService geminiService;

    public FunService(FunCategoryRepository categoryRepository,
            FunContentRepository contentRepository,
            UserFunViewRepository userViewRepository,
            UserProfileRepository userProfileRepository,
            GeminiService geminiService) {
        this.categoryRepository = categoryRepository;
        this.contentRepository = contentRepository;
        this.userViewRepository = userViewRepository;
        this.userProfileRepository = userProfileRepository;
        this.geminiService = geminiService;
    }

    public FunContent getNextFunForCurrentUser() {
        UserProfile user = getCurrentUserProfile();
        if (user == null) {
            logger.warn("No authenticated user found for fun fetch.");
            return null;
        }

        // 1. Try to find an unseen fun item in DB
        Optional<FunContent> existing = contentRepository.findRandomUnseen(user.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. If none, generate new fun items via Gemini
        logger.info("No unseen fun content for user {}, generating more...", user.getId());
        generateAndSaveFun(10);

        // 3. Try fetching again
        return contentRepository.findRandomUnseen(user.getId()).orElse(null);
    }

    public void markFunAsViewed(Long funId, Boolean isLiked) {
        UserProfile user = getCurrentUserProfile();
        if (user == null)
            return;

        try {
            UserFunView view = new UserFunView(user.getId(), funId, isLiked);
            userViewRepository.save(view);
        } catch (Exception e) {
            logger.warn("Failed to mark fun {} as viewed for user {}: {}", funId, user.getId(), e.getMessage());
        }
    }

    private void generateAndSaveFun(int count) {
        List<String> newItems = geminiService.generateFun(count);
        if (newItems.isEmpty())
            return;

        // Ensure default category exists
        FunCategory category = categoryRepository.findByName("Anecdotes")
                .orElseGet(() -> categoryRepository.save(new FunCategory("Anecdotes", "Fun anecdotes and gags")));

        for (String content : newItems) {
            FunContent funContent = new FunContent(category.getId(), content, "Gemini");
            contentRepository.save(funContent);
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
