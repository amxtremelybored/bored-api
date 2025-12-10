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
    private final GeminiService geminiService;

    @Transactional
    public QuizResponse getNextPuzzleForCurrentUser() {
        UserProfile user = getCurrentUserProfile();
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // 1. Try to find an unseen puzzle in DB
        List<PuzzleContent> unseen = contentRepository.findRandomUnseen(user.getId());

        PuzzleContent puzzle;
        if (!unseen.isEmpty()) {
            puzzle = unseen.get(0);
            log.info("Found existing unseen puzzle ID: {}", puzzle.getId());
        } else {
            // 2. Fallback: Generate new puzzles via Gemini
            log.info("No unseen puzzles found for user {}. Generating via Gemini...", user.getId());
            puzzle = generateAndSavePuzzles();
        }

        if (puzzle == null) {
            return null; // Should not happen if Gemini works
        }

        // 3. Mark as viewed immediately
        markAsViewedInternal(user, puzzle, null);

        // 4. Convert to DTO
        QuizResponse response = new QuizResponse();
        response.setId(puzzle.getId());
        response.setCategoryId(puzzle.getCategory().getId()); // Fix: Pass UUID directly
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

    private PuzzleContent generateAndSavePuzzles() {
        // 1. Get or create default category
        PuzzleCategory category = categoryRepository.findAll().stream()
                .filter(c -> "Generic Puzzles".equals(c.getName()))
                .findFirst()
                .orElseGet(() -> {
                    PuzzleCategory c = new PuzzleCategory();
                    c.setId(UUID.randomUUID());
                    c.setName("Generic Puzzles");
                    c.setEmoji("🧩");
                    c.setDescription("Fun generic puzzles!");
                    return categoryRepository.save(c);
                });

        // 2. Call Gemini
        int count = 10;
        List<QuizResponse> generated = geminiService.generatePuzzle(count);

        if (generated.isEmpty())
            return null;

        PuzzleContent first = null;

        for (QuizResponse dto : generated) {
            PuzzleContent content = new PuzzleContent();
            content.setCategory(category);
            content.setQuestion(dto.getQuestion());
            content.setAnswer(dto.getAnswer());
            content.setOptions(dto.getOptions()); // JSON string
            content.setDifficultyLevel(1);

            content = contentRepository.save(content);
            if (first == null)
                first = content;
        }

        return first;
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
