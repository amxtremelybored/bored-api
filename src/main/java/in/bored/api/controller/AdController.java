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

    public AdController(AdService adService) {
        this.adService = adService;
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
    public ResponseEntity<AdResponse> serveAd(@RequestParam Long userProfileId) {
        AdResponse ad = adService.serveAd(userProfileId);
        if (ad == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(ad);
    }
}
