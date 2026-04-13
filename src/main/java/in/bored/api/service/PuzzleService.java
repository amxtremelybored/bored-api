package in.bored.api.service;

import in.bored.api.dto.QuizResponse;
import in.bored.api.model.ProfileStatus;
import in.bored.api.model.PuzzleCategory;
import in.bored.api.model.PuzzleContent;
import in.bored.api.model.UserProfile;
import in.bored.api.model.UserPuzzleView;
import in.bored.api.repo.PuzzleCategoryRepository;
import in.bored.api.repo.PuzzleContentRepository;
import in.bored.api.repo.UserPuzzleViewRepository;
import in.bored.api.repo.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PuzzleService {

    private final PuzzleCategoryRepository categoryRepository;
    private final PuzzleContentRepository contentRepository;
    private final UserPuzzleViewRepository viewRepository;
    private final UserProfileRepository userProfileRepository;


    @Transactional
    public java.util.List<QuizResponse> getNextPuzzlesForCurrentUser(int count) {
        UserProfile user = getCurrentUserProfile();
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        log.info("Fetching {} puzzles for user {}", count, user.getId());

        // 1. Try to find an unseen puzzle in DB
        List<PuzzleContent> unseen = contentRepository.findRandomUnseen(user.getId(), count);
        log.info("Found {} unseen puzzles in DB", unseen.size());



        // 5. Mark all as served/viewed to prevent duplicates
        for (PuzzleContent pc : unseen) {
            if (!viewRepository.existsByUserProfileAndPuzzleContent(user, pc)) {
                UserPuzzleView view = new UserPuzzleView();
                view.setUserProfile(user);
                view.setPuzzleContent(pc);
                view.setIsCorrect(null);
                viewRepository.save(view);
            }
        }

        // 6. Convert to DTO
        return unseen.stream().map(this::toResponse).collect(java.util.stream.Collectors.toList());
    }

    private QuizResponse toResponse(PuzzleContent puzzle) {
        QuizResponse response = new QuizResponse();
        response.setId(puzzle.getId());
        response.setCategoryId(puzzle.getCategory().getId());
        response.setCategoryName(puzzle.getCategory().getName());
        response.setQuestion(puzzle.getQuestion());
        response.setAnswer(puzzle.getAnswer());
        response.setOptions(puzzle.getOptions());
        response.setDifficultyLevel(puzzle.getDifficultyLevel());
        return response;
    }

    @Transactional
    public void markPuzzleAsViewed(Long puzzleId, Boolean isCorrect) {
        UserProfile user = getCurrentUserProfile();
        if (user == null)
            return;

        Optional<PuzzleContent> puzzleOpt = contentRepository.findById(puzzleId);
        if (puzzleOpt.isEmpty())
            return;

        PuzzleContent puzzle = puzzleOpt.get();
        markAsViewedInternal(user, puzzle, isCorrect);
    }

    private void markAsViewedInternal(UserProfile user, PuzzleContent puzzle, Boolean isCorrect) {
        if (viewRepository.existsByUserProfileAndPuzzleContent(user, puzzle)) {
            return;
        }
        try {
            UserPuzzleView view = new UserPuzzleView();
            view.setUserProfile(user);
            view.setPuzzleContent(puzzle);
            view.setIsCorrect(isCorrect);
            viewRepository.save(view);
        } catch (Exception e) {
            log.warn("Could not save view (likely exists): {}", e.getMessage());
        }
    }



    private UserProfile getCurrentUserProfile() {
        String uid = getCurrentUid();
        return userProfileRepository.findByUid(uid)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUid(uid);
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
