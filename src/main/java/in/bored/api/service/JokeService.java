package in.bored.api.service;

import in.bored.api.model.*;
import in.bored.api.repo.JokeCategoryRepository;
import in.bored.api.repo.JokeContentRepository;
import in.bored.api.repo.UserJokeViewRepository;
import in.bored.api.repo.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JokeService {

    private static final Logger logger = LoggerFactory.getLogger(JokeService.class);

    private final JokeCategoryRepository categoryRepository;
    private final JokeContentRepository contentRepository;
    private final UserJokeViewRepository userViewRepository;
    private final UserProfileRepository userProfileRepository;
    public JokeService(JokeCategoryRepository categoryRepository,
            JokeContentRepository contentRepository,
            UserJokeViewRepository userViewRepository,
            UserProfileRepository userProfileRepository) {
        this.categoryRepository = categoryRepository;
        this.contentRepository = contentRepository;
        this.userViewRepository = userViewRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public List<JokeContent> getNextJokeForCurrentUser(int count) {
        UserProfile user = getCurrentUserProfile();
        if (user == null) {
            logger.warn("No authenticated user found for joke fetch.");
            return List.of();
        }

        // 1. Try to find unseen jokes in DB
        List<JokeContent> finalResult = contentRepository.findRandomUnseen(user.getId(), count);

        // 4. Mark all as served/viewed to prevent duplicates
        for (JokeContent jc : finalResult) {
            if (!userViewRepository.existsByUserProfileIdAndJokeContentId(user.getId(), jc.getId())) {
                try {
                    UserJokeView view = new UserJokeView(user.getId(), jc.getId(), null);
                    userViewRepository.save(view);
                } catch (Exception e) {
                    logger.warn("Could not save joke view: {}", e.getMessage());
                }
            }
        }

        return finalResult;
    }

    public void markJokeAsViewed(Long jokeId, Boolean isLiked) {
        UserProfile user = getCurrentUserProfile();
        if (user == null)
            return;

        try {
            UserJokeView view = new UserJokeView(user.getId(), jokeId, isLiked);
            userViewRepository.save(view);
        } catch (Exception e) {
            logger.warn("Failed to mark joke {} as viewed for user {}: {}", jokeId, user.getId(), e.getMessage());
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
