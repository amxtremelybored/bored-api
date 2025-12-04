package in.bored.api.controller;

import in.bored.api.dto.QuizResponse;
import in.bored.api.service.PuzzleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/puzzle")
@RequiredArgsConstructor
@Slf4j
public class PuzzleController {

    private final PuzzleService puzzleService;

    @GetMapping("/next")
    public ResponseEntity<QuizResponse> getNextPuzzle() {
        QuizResponse response = puzzleService.getNextPuzzleForCurrentUser();
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> markPuzzleAsViewed(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {

        Boolean isCorrect = body.get("isCorrect");
        puzzleService.markPuzzleAsViewed(id, isCorrect);
        return ResponseEntity.ok().build();
    }
}
