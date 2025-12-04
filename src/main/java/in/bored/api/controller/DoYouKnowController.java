package in.bored.api.controller;

import in.bored.api.model.DoYouKnowContent;
import in.bored.api.service.DoYouKnowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/doyouknow")
public class DoYouKnowController {

    private final DoYouKnowService service;

    public DoYouKnowController(DoYouKnowService service) {
        this.service = service;
    }

    @GetMapping("/next")
    public ResponseEntity<DoYouKnowContent> getNextFact() {
        DoYouKnowContent content = service.getNextDoYouKnowForCurrentUser();
        if (content == null) {
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
