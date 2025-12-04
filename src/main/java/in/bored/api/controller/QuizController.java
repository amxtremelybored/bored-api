package in.bored.api.controller;

import in.bored.api.dto.QuizResponse;
import in.bored.api.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/next")
    public ResponseEntity<QuizResponse> getNextQuiz() {
        QuizResponse quiz = quizService.getNextQuizForCurrentUser();
        if (quiz == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(quiz);
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> markQuizAsViewed(@PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> payload) {
        Boolean isCorrect = (payload != null) ? payload.get("isCorrect") : null;
        quizService.markQuizAsViewed(id, isCorrect);
        return ResponseEntity.ok().build();
    }
}
