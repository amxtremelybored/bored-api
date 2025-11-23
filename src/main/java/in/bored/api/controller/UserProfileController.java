// src/main/java/in/bored/api/controller/UserProfileController.java
package in.bored.api.controller;

import in.bored.api.dto.UserProfileRequest;
import in.bored.api.model.UserProfile;
import in.bored.api.service.UserProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
    public ResponseEntity<UserProfile> getById(@PathVariable UUID id) {
        UserProfile profile = userProfileService.getById(id);
        return ResponseEntity.ok(profile);
    }

    @GetMapping
    public ResponseEntity<Page<UserProfile>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserProfile> result = userProfileService.list(pageable);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> update(
            @PathVariable UUID id,
            @RequestBody UserProfileRequest request
    ) {
        UserProfile updated = userProfileService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        userProfileService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getCurrentUserProfile() {
        UserProfile profile = userProfileService.getCurrentUserProfile();
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/me")
    public ResponseEntity<UserProfile> upsertCurrentUserProfile(@RequestBody UserProfileRequest request) {
        UserProfile profile = userProfileService.upsertCurrentUserProfile(request);
        return ResponseEntity.ok(profile);
    }
}