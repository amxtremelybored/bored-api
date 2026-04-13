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
    public FunService(FunCategoryRepository categoryRepository,
            FunContentRepository contentRepository,
            UserFunViewRepository userViewRepository,
            UserProfileRepository userProfileRepository) {
        this.categoryRepository = categoryRepository;
        this.contentRepository = contentRepository;
        this.userViewRepository = userViewRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public List<FunContent> getNextFunForCurrentUser(int count) {
        UserProfile user = getCurrentUserProfile();
        if (user == null) {
            logger.warn("No authenticated user found for fun fetch.");
            return List.of();
        }

        // 1. Try to find unseen fun items in DB
        List<FunContent> finalResult = contentRepository.findRandomUnseen(user.getId(), count);

        // 4. Mark all as served/viewed to prevent duplicates
        for (FunContent fc : finalResult) {
            if (!userViewRepository.existsByUserProfileIdAndFunContentId(user.getId(), fc.getId())) {
                try {
                    UserFunView view = new UserFunView(user.getId(), fc.getId(), null);
                    userViewRepository.save(view);
                } catch (Exception e) {
                    logger.warn("Could not save fun view: {}", e.getMessage());
                }
            }
        }

        return finalResult;
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



    private UserProfile getCurrentUserProfile() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return null;
        String uid = auth.getName();
        return userProfileRepository.findByUidAndStatusNot(uid, ProfileStatus.DELETED).orElse(null);
    }
}
