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

    public java.util.List<QuizResponse> getNextQuizzesForCurrentUser(int count) {
        UserProfile profile = getCurrentUserProfile();

        // 1. Try to find random unseen quiz from DB
        java.util.List<QuizContent> existing = quizContentRepository.findRandomUnseen(profile.getId(), count);

        // 2. Fallback: Generate from Gemini if needed
        if (existing.size() < count) {
            // Pick a random category name or topic.
            String topicName = "General Knowledge";
            long catCount = quizCategoryRepository.count();
            if (catCount > 0) {
                int idx = (int) (Math.random() * catCount);
                org.springframework.data.domain.Page<in.bored.api.model.QuizCategory> page = quizCategoryRepository
                        .findAll(org.springframework.data.domain.PageRequest.of(idx, 1));
                if (page.hasContent()) {
                    topicName = page.getContent().get(0).getName();
                }
            }

            // Generate batch
            // We need (count - existing.size()) more, but let's ask for 10 at minimum to be
            // safe
            int numToGen = Math.max(10, count - existing.size());

            // Use a random set number to vary content
            int setNumber = (int) (Math.random() * 100) + 1;

            java.util.List<QuizResponse> generated = geminiService.generateQuiz(topicName, numToGen, setNumber);

            if (!generated.isEmpty()) {
                // 3. Save generated quizzes
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

                for (QuizResponse dto : generated) {
                    Optional<QuizContent> dupeCheck = quizContentRepository.findByQuestion(dto.getQuestion());
                    if (dupeCheck.isEmpty()) {
                        QuizContent qc = new QuizContent();
                        qc.setCategory(category);
                        qc.setQuestion(dto.getQuestion());
                        qc.setAnswer(dto.getAnswer());
                        qc.setOptions(dto.getOptions());
                        qc.setDifficultyLevel(dto.getDifficultyLevel());
                        quizContentRepository.save(qc);
                    }
                }

                // 4. Refetch to get IDs and any missed ones
                existing = quizContentRepository.findRandomUnseen(profile.getId(), count);
            }
        }

        // 5. Mark all as served/viewed to prevent duplicates
        for (QuizContent qc : existing) {
            if (!userQuizViewRepository.existsByUserProfileAndQuizContent(profile, qc)) {
                UserQuizView view = new UserQuizView();
                view.setUserProfile(profile);
                view.setQuizContent(qc);
                view.setIsCorrect(null); // Not answered yet, just served
                userQuizViewRepository.save(view);
            }
        }

        return existing.stream().map(this::toResponse).collect(java.util.stream.Collectors.toList());
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
