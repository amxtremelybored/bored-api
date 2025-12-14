package in.bored.api.service;

import in.bored.api.dto.QuizResponse;
import in.bored.api.model.ProfileStatus;
import in.bored.api.model.QuizCategory;
import in.bored.api.model.QuizContent;
import in.bored.api.model.UserProfile;
import in.bored.api.model.UserQuizView;
import in.bored.api.repo.QuizCategoryRepository;
import in.bored.api.repo.QuizContentRepository;
import in.bored.api.repo.UserProfileRepository;
import in.bored.api.repo.UserQuizViewRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class QuizService {

    private final QuizContentRepository quizContentRepository;
    private final UserQuizViewRepository userQuizViewRepository;
    private final UserProfileRepository userProfileRepository;

    private final QuizCategoryRepository quizCategoryRepository;
    private final GeminiService geminiService;

    public QuizService(QuizContentRepository quizContentRepository,
            UserQuizViewRepository userQuizViewRepository,
            UserProfileRepository userProfileRepository,
            QuizCategoryRepository quizCategoryRepository,
            GeminiService geminiService) {
        this.quizContentRepository = quizContentRepository;
        this.userQuizViewRepository = userQuizViewRepository;
        this.userProfileRepository = userProfileRepository;
        this.quizCategoryRepository = quizCategoryRepository;
        this.geminiService = geminiService;
    }

    public QuizResponse getNextQuizForCurrentUser() {
        UserProfile profile = getCurrentUserProfile();

        // 1. Try to find random unseen quiz from DB
        Optional<QuizContent> quizOpt = quizContentRepository.findRandomUnseen(profile);

        if (quizOpt.isPresent()) {
            return toResponse(quizOpt.get());
        }

        // 2. Fallback: Generate from Gemini
        // Pick a random category name or topic. For now, let's pick a random existing
        // category if any,
        // or default to "General Knowledge"
        String topicName = "General Knowledge";
        long count = quizCategoryRepository.count();
        if (count > 0) {
            int idx = (int) (Math.random() * count);
            org.springframework.data.domain.Page<in.bored.api.model.QuizCategory> page = quizCategoryRepository
                    .findAll(org.springframework.data.domain.PageRequest.of(idx, 1));
            if (page.hasContent()) {
                topicName = page.getContent().get(0).getName();
            }
        }

        // Generate 5 questions for set 1 (can randomize set number later)
        int setNumber = (int) (Math.random() * 100) + 1;
        java.util.List<QuizResponse> generated = geminiService.generateQuiz(topicName, 5, setNumber);

        if (generated.isEmpty()) {
            return null;
        }

        // 3. Save generated quizzes
        // Ensure category exists
        String finalTopicName = topicName;
        in.bored.api.model.QuizCategory category = quizCategoryRepository.findAll().stream()
                .filter(c -> c.getName().equalsIgnoreCase(finalTopicName))
                .findFirst()
                .orElseGet(() -> {
                    in.bored.api.model.QuizCategory newCat = new in.bored.api.model.QuizCategory();
                    newCat.setName(finalTopicName);
                    newCat.setEmoji("❓"); // Default emoji
                    return quizCategoryRepository.save(newCat);
                });

        java.util.List<QuizContent> savedQuizzes = new java.util.ArrayList<>();
        for (QuizResponse dto : generated) {
            Optional<QuizContent> existing = quizContentRepository.findByQuestion(dto.getQuestion());
            if (existing.isPresent()) {
                savedQuizzes.add(existing.get());
            } else {
                QuizContent qc = new QuizContent();
                qc.setCategory(category);
                qc.setQuestion(dto.getQuestion());
                qc.setAnswer(dto.getAnswer());
                qc.setOptions(dto.getOptions());
                qc.setDifficultyLevel(dto.getDifficultyLevel());
                savedQuizzes.add(quizContentRepository.save(qc));
            }
        }

        // 4. Mark the FIRST one as viewed immediately and return it, IF UNSEEN
        QuizContent firstQuiz = savedQuizzes.get(0);

        if (!userQuizViewRepository.existsByUserProfileAndQuizContent(profile, firstQuiz)) {
            UserQuizView view = new UserQuizView();
            view.setUserProfile(profile);
            view.setQuizContent(firstQuiz);
            // isCorrect is null initially
            userQuizViewRepository.save(view);
        }

        return toResponse(firstQuiz);
    }

    @Transactional
    public void markQuizAsViewed(Long quizId, Boolean isCorrect) {
        UserProfile profile = getCurrentUserProfile();
        QuizContent quiz = quizContentRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));

        UserQuizView view = new UserQuizView();
        view.setUserProfile(profile);
        view.setQuizContent(quiz);
        view.setIsCorrect(isCorrect);

        userQuizViewRepository.save(view);
    }

    private QuizResponse toResponse(QuizContent quiz) {
        QuizResponse dto = new QuizResponse();
        dto.setId(quiz.getId());
        dto.setCategoryId(quiz.getCategory().getId());
        dto.setCategoryName(quiz.getCategory().getName());
        dto.setQuestion(quiz.getQuestion());
        dto.setAnswer(quiz.getAnswer());
        dto.setOptions(quiz.getOptions());
        dto.setDifficultyLevel(quiz.getDifficultyLevel());
        return dto;
    }

    private UserProfile getCurrentUserProfile() {
        String uid = getCurrentUid();
        return userProfileRepository.findByUidAndStatusNot(uid, ProfileStatus.DELETED)
                .orElseGet(() -> {
                    // Auto-create profile for new users
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUid(uid);
                    newProfile.setFirebaseUid(uid);
                    newProfile.setStatus(ProfileStatus.ACTIVE);
                    return userProfileRepository.save(newProfile);
                });
    }

    private String getCurrentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return auth.getPrincipal().toString();
    }
}
