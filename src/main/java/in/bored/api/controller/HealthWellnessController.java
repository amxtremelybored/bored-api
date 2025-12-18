package in.bored.api.controller;

import in.bored.api.model.HealthWellnessContent;
import in.bored.api.model.UserProfile;
import in.bored.api.service.HealthWellnessService;
import in.bored.api.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/health-wellness")
public class HealthWellnessController {

    private final HealthWellnessService healthWellnessService;
    private final UserProfileService userProfileService;

    public HealthWellnessController(HealthWellnessService healthWellnessService,
            UserProfileService userProfileService) {
        this.healthWellnessService = healthWellnessService;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/next")
    public ResponseEntity<?> getNextTips(Authentication authentication, @RequestParam String topic,
            @RequestParam(defaultValue = "10") int count) {
        try {
            UserProfile user = userProfileService.getCurrentUserProfile();
            java.util.List<HealthWellnessContent> content = healthWellnessService.getNextTips(user.getId(), topic,
                    count);
            if (content.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<?> markAsViewed(Authentication authentication, @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        try {
            UserProfile user = userProfileService.getCurrentUserProfile();
            Boolean isLiked = body.getOrDefault("isLiked", false);
            healthWellnessService.markAsViewed(user.getId(), id, isLiked);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
}
