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

    @PostMapping
    public ResponseEntity<UserPreference> create(@RequestBody UserPreferenceRequest request) {
        UserPreference created = userPreferenceService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Bulk REPLACE: overwrite the current user's preferences
     * with exactly the given categoryIds.
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<UserPreference>> createBulk(
            @RequestBody UserPreferenceBulkRequest request
    ) {
        List<UserPreference> replaced =
                userPreferenceService.replacePreferencesForCurrentUser(
                        request.getCategoryIds() == null
                                ? List.of()
                                : request.getCategoryIds()
                );
        return ResponseEntity.ok(replaced);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserPreference> getById(@PathVariable Long id) {
        UserPreference pref = userPreferenceService.getById(id);
        return ResponseEntity.ok(pref);
    }

    @GetMapping("/me")
    public ResponseEntity<List<UserPreference>> getCurrentUserPreferences() {
        List<UserPreference> prefs = userPreferenceService.getCurrentUserPreferences();
        return ResponseEntity.ok(prefs);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userPreferenceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}