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

    public List<FunContent> getNextFunForCurrentUser(int count) {
        UserProfile user = getCurrentUserProfile();
        if (user == null) {
            logger.warn("No authenticated user found for fun fetch.");
            return List.of();
        }

        // 1. Try to find unseen fun items in DB
        List<FunContent> existing = contentRepository.findRandomUnseen(user.getId(), count);
        if (existing.size() >= count) {
            return existing;
        }

        // 2. If not enough, generate new fun items via Gemini
        // Calculate how many more we need, but maybe just generate a batch of 10 anyway
        // to be safe/efficient
        logger.info("Not enough unseen fun content for user {} (found {}), generating more...", user.getId(),
                existing.size());
        generateAndSaveFun(10);

        // 3. Try fetching again
        return contentRepository.findRandomUnseen(user.getId(), count);
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
