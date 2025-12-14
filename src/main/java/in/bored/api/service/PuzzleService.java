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
    public java.util.List<QuizResponse> getNextPuzzlesForCurrentUser(int count) {
        UserProfile user = getCurrentUserProfile();
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        log.info("Fetching {} puzzles for user {}", count, user.getId());

        // 1. Try to find an unseen puzzle in DB
        List<PuzzleContent> unseen = contentRepository.findRandomUnseen(user.getId(), count);
        log.info("Found {} unseen puzzles in DB", unseen.size());

        // If we found enough, return them
        if (unseen.size() >= count) {
            return unseen.stream().map(this::toResponse).collect(java.util.stream.Collectors.toList());
        }

        // 2. Fallback: Generate new puzzles via Gemini
        int numToGen = Math.max(10, count - unseen.size());
        log.info("Not enough unseen puzzles for user {}. Generating {} via Gemini...", user.getId(), numToGen);
        java.util.List<PuzzleContent> generated = generateAndSavePuzzles(numToGen);
        log.info("Generated {} new puzzles", generated.size());

        if (generated != null && !generated.isEmpty()) {
            // Re-fetch to ensure we have IDs and respect limit
            unseen = contentRepository.findRandomUnseen(user.getId(), count);
            log.info("After generation, found {} unseen puzzles in DB", unseen.size());
        }

        // 4. Convert to DTO
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

    private java.util.List<PuzzleContent> generateAndSavePuzzles(int count) {
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
        List<QuizResponse> generated = geminiService.generatePuzzle(count);

        if (generated.isEmpty())
            return java.util.Collections.emptyList();

        java.util.List<PuzzleContent> savedList = new java.util.ArrayList<>();

        for (QuizResponse dto : generated) {
            Optional<PuzzleContent> existing = contentRepository.findByQuestion(dto.getQuestion());
            PuzzleContent content;

            if (existing.isPresent()) {
                content = existing.get();
            } else {
                content = new PuzzleContent();
                content.setCategory(category);
                content.setQuestion(dto.getQuestion());
                content.setAnswer(dto.getAnswer());
                content.setOptions(dto.getOptions()); // JSON string
                content.setDifficultyLevel(1);
                content = contentRepository.save(content);
            }
            savedList.add(content);
        }

        return savedList;
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
