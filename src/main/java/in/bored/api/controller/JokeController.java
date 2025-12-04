package in.bored.api.controller;

import in.bored.api.model.JokeContent;
import in.bored.api.service.JokeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/joke")
public class JokeController {

    private final JokeService jokeService;

    public JokeController(JokeService jokeService) {
        this.jokeService = jokeService;
    }

    @GetMapping("/next")
    public ResponseEntity<JokeContent> getNextJoke() {
        JokeContent joke = jokeService.getNextJokeForCurrentUser();
        if (joke == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(joke);
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> markJokeAsViewed(@PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> payload) {
        Boolean isLiked = (payload != null) ? payload.get("isLiked") : null;
        jokeService.markJokeAsViewed(id, isLiked);
        return ResponseEntity.ok().build();
    }
}
