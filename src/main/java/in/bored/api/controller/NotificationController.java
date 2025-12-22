package in.bored.api.controller;

import in.bored.api.model.NotificationContent;
import in.bored.api.repo.NotificationContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationContentRepository notificationContentRepository;

    @GetMapping("/random")
    public ResponseEntity<NotificationContent> getRandomUnviewed() {
        return notificationContentRepository.findRandomUnviewed()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> markAsViewed(@PathVariable Long id) {
        return notificationContentRepository.findById(id)
                .map(notification -> {
                    notification.setViewed(true);
                    notificationContentRepository.save(notification);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
