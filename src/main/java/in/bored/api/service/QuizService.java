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
    public QuizService(QuizContentRepository quizContentRepository,
            UserQuizViewRepository userQuizViewRepository,
            UserProfileRepository userProfileRepository,
            QuizCategoryRepository quizCategoryRepository) {
        this.quizContentRepository = quizContentRepository;
        this.userQuizViewRepository = userQuizViewRepository;
        this.userProfileRepository = userProfileRepository;
        this.quizCategoryRepository = quizCategoryRepository;
    }

    public java.util.List<QuizResponse> getNextQuizzesForCurrentUser(int count) {
        UserProfile profile = getCurrentUserProfile();

        // 1. Try to find random unseen quiz from DB
        java.util.List<QuizContent> existing = quizContentRepository.findRandomUnseen(profile.getId(), count);


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
