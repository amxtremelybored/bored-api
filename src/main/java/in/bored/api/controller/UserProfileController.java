// src/main/java/in/bored/api/controller/UserProfileController.java
package in.bored.api.controller;

import in.bored.api.dto.UserProfileRequest;
import in.bored.api.model.UserProfile;
import in.bored.api.service.UserProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-profiles")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PostMapping
    public ResponseEntity<UserProfile> create(@RequestBody UserProfileRequest request) {
        UserProfile created = userProfileService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> getById(@PathVariable Long id) {
        UserProfile profile = userProfileService.getById(id);
        return ResponseEntity.ok(profile);
    }

    @GetMapping
    public ResponseEntity<Page<UserProfile>> list(Pageable pageable) {
        Page<UserProfile> page = userProfileService.list(pageable);
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> update(@PathVariable Long id,
                                              @RequestBody UserProfileRequest request) {
        UserProfile updated = userProfileService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable Long id) {
        userProfileService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    // Firebase UID based endpoints

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getMe() {
        UserProfile profile = userProfileService.getCurrentUserProfile();
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfile> upsertMe(@RequestBody UserProfileRequest request) {
        UserProfile profile = userProfileService.upsertCurrentUserProfile(request);
        return ResponseEntity.ok(profile);
    }
}