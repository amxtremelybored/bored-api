package in.bored.api.controller;

import in.bored.api.model.FunContent;
import in.bored.api.service.FunService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fun")
public class FunController {

    private final FunService funService;

    public FunController(FunService funService) {
        this.funService = funService;
    }

    @GetMapping("/next")
    public ResponseEntity<List<FunContent>> getNextFun(@RequestParam(defaultValue = "10") int size) {
        List<FunContent> fun = funService.getNextFunForCurrentUser(size);
        if (fun.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(fun);
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> markFunAsViewed(@PathVariable Long id,
            @RequestBody(required = false) Map<String, Boolean> payload) {
        Boolean isLiked = (payload != null) ? payload.get("isLiked") : null;
        funService.markFunAsViewed(id, isLiked);
        return ResponseEntity.ok().build();
    }
}
