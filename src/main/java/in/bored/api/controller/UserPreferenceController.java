// src/main/java/in/bored/api/controller/UserPreferenceController.java
package in.bored.api.controller;

import in.bored.api.dto.UserPreferenceBulkRequest;
import in.bored.api.dto.UserPreferenceRequest;
import in.bored.api.model.UserPreference;
import in.bored.api.service.UserPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-preferences")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    public UserPreferenceController(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    // CREATE single preference for CURRENT user
    @PostMapping
    public ResponseEntity<UserPreference> create(@RequestBody UserPreferenceRequest request) {
        UserPreference created = userPreferenceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // BULK CREATE for CURRENT user (multiple topicIds)
    @PostMapping("/bulk")
    public ResponseEntity<List<UserPreference>> createBulk(@RequestBody UserPreferenceBulkRequest request) {
        List<UserPreference> created =
                userPreferenceService.addPreferencesForCurrentUser(request.getTopicIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // READ ONE by preference id
    @GetMapping("/{id}")
    public ResponseEntity<UserPreference> getById(@PathVariable Long id) {
        UserPreference pref = userPreferenceService.getById(id);
        return ResponseEntity.ok(pref);
    }

    // LIST preferences for CURRENT user
    @GetMapping("/me")
    public ResponseEntity<List<UserPreference>> getCurrentUserPreferences() {
        List<UserPreference> prefs = userPreferenceService.getCurrentUserPreferences();
        return ResponseEntity.ok(prefs);
    }

    // DELETE (hard)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userPreferenceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}