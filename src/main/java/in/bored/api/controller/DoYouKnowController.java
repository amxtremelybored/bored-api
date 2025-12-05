package in.bored.api.controller;

import in.bored.api.model.DoYouKnowContent;
import in.bored.api.service.DoYouKnowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doyouknow")
public class DoYouKnowController {

    private final DoYouKnowService service;

    public DoYouKnowController(DoYouKnowService service) {
        this.service = service;
    }

    @GetMapping("/next")
    public ResponseEntity<List<DoYouKnowContent>> getNextFact(@RequestParam(defaultValue = "10") int size) {
        List<DoYouKnowContent> content = service.getNextDoYouKnowForCurrentUser(size);
        if (content.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(content);
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> markFactAsViewed(@PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> payload) {
        Boolean isLiked = (payload != null) ? payload.get("isLiked") : null;
        service.markDoYouKnowAsViewed(id, isLiked);
        return ResponseEntity.ok().build();
    }
}
