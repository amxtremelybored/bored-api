package in.bored.api.controller;

import in.bored.api.dto.*;
import in.bored.api.service.AdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ads")
public class AdController {

    private final AdService adService;
    private final in.bored.api.service.UserProfileService userProfileService;

    public AdController(AdService adService, in.bored.api.service.UserProfileService userProfileService) {
        this.adService = adService;
        this.userProfileService = userProfileService;
    }

    // --- Ad CRUD ---

    @GetMapping
    public ResponseEntity<List<AdResponse>> getAllAds() {
        return ResponseEntity.ok(adService.getAllAds());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdResponse> getAdById(@PathVariable UUID id) {
        return ResponseEntity.ok(adService.getAdById(id));
    }

    @PostMapping
    public ResponseEntity<AdResponse> createAd(@RequestBody AdRequest request) {
        return ResponseEntity.ok(adService.createAd(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdResponse> updateAd(@PathVariable UUID id, @RequestBody AdRequest request) {
        return ResponseEntity.ok(adService.updateAd(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAd(@PathVariable UUID id) {
        adService.deleteAd(id);
        return ResponseEntity.noContent().build();
    }

    // --- Rules ---

    @GetMapping("/{id}/rules")
    public ResponseEntity<List<AdTargetingRuleResponse>> getRules(@PathVariable UUID id) {
        return ResponseEntity.ok(adService.getRules(id));
    }

    @PostMapping("/{id}/rules")
    public ResponseEntity<AdTargetingRuleResponse> addRule(@PathVariable UUID id,
            @RequestBody AdTargetingRuleRequest request) {
        return ResponseEntity.ok(adService.addRule(id, request));
    }

    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        adService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    // --- Serving ---

    @GetMapping("/serve")
    public ResponseEntity<AdResponse> serveAd() {
        // Authenticated user resolution
        in.bored.api.model.UserProfile currentUser = userProfileService.getCurrentUserProfile();

        AdResponse ad = adService.serveAd(currentUser.getId());
        if (ad == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ad);
    }

    // DEBUG: List files in ad media directory
    @GetMapping("/debug-files")
    public ResponseEntity<java.util.List<String>> listFiles() {
        java.io.File folder = new java.io.File("/home/bored/ad/");
        java.io.File[] listOfFiles = folder.listFiles();
        java.util.List<String> files = new java.util.ArrayList<>();

        if (listOfFiles != null) {
            for (java.io.File file : listOfFiles) {
                files.add(file.getName() + (file.isDirectory() ? "/" : "") + " (" + file.length() + " bytes)");
            }
        } else {
            files.add("Directory does not exist or IO error");
        }
        return ResponseEntity.ok(files);
    }
}
