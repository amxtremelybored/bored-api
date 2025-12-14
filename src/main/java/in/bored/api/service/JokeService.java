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
    private final GeminiService geminiService;

    public JokeService(JokeCategoryRepository categoryRepository,
            JokeContentRepository contentRepository,
            UserJokeViewRepository userViewRepository,
            UserProfileRepository userProfileRepository,
            GeminiService geminiService) {
        this.categoryRepository = categoryRepository;
        this.contentRepository = contentRepository;
        this.userViewRepository = userViewRepository;
        this.userProfileRepository = userProfileRepository;
        this.geminiService = geminiService;
    }

    public List<JokeContent> getNextJokeForCurrentUser(int count) {
        UserProfile user = getCurrentUserProfile();
        if (user == null) {
            logger.warn("No authenticated user found for joke fetch.");
            return List.of();
        }

        // 1. Try to find unseen jokes in DB
        // 1. Try to find unseen jokes in DB
        List<JokeContent> existing = contentRepository.findRandomUnseen(user.getId(), count);
        if (existing.size() >= count) {
            return existing;
        }

        // 2. If not enough, generate new jokes via Gemini
        logger.info("Not enough unseen jokes for user {} (found {}), generating more...", user.getId(),
                existing.size());
        generateAndSaveJokes(10); // Deduplicates internally

        // 3. Try fetching again (should return new unseen, or existing unseen)
        return contentRepository.findRandomUnseen(user.getId(), count);
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

    private void generateAndSaveJokes(int count) {
        List<JokeContent> newJokes = geminiService.generateJoke(count);
        if (newJokes.isEmpty())
            return;

        // Ensure default category exists
        JokeCategory category = categoryRepository.findByName("Generic Jokes")
                .orElseGet(() -> categoryRepository.save(new JokeCategory("Generic Jokes", "General purpose jokes")));

        for (JokeContent joke : newJokes) {
            java.util.Optional<JokeContent> existing = contentRepository.findBySetupAndPunchline(joke.getSetup(),
                    joke.getPunchline());
            if (existing.isEmpty()) {
                joke.setCategoryId(category.getId());
                contentRepository.save(joke);
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
