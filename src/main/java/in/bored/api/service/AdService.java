package in.bored.api.service;

import in.bored.api.dto.*;
import in.bored.api.model.*;
import in.bored.api.repo.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdService {

    private final AdRepository adRepository;
    private final AdTargetingRuleRepository ruleRepository;
    private final AdImpressionRepository impressionRepository;
    private final UserProfileRepository userProfileRepository;
    private final Random random = new Random();

    public AdService(AdRepository adRepository,
            AdTargetingRuleRepository ruleRepository,
            AdImpressionRepository impressionRepository,
            UserProfileRepository userProfileRepository) {
        this.adRepository = adRepository;
        this.ruleRepository = ruleRepository;
        this.impressionRepository = impressionRepository;
        this.userProfileRepository = userProfileRepository;
    }

    // --- Ad CRUD ---

    public List<AdResponse> getAllAds() {
        return adRepository.findAll().stream()
                .map(this::mapToAdResponse)
                .collect(Collectors.toList());
    }

    public AdResponse getAdById(UUID id) {
        return mapToAdResponse(findAd(id));
    }

    @Transactional
    public AdResponse createAd(AdRequest request) {
        Ad ad = new Ad();
        updateAdFromRequest(ad, request);
        return mapToAdResponse(adRepository.save(ad));
    }

    @Transactional
    public AdResponse updateAd(UUID id, AdRequest request) {
        Ad ad = findAd(id);
        updateAdFromRequest(ad, request);
        return mapToAdResponse(adRepository.save(ad));
    }

    @Transactional
    public void deleteAd(UUID id) {
        adRepository.deleteById(id);
    }

    // --- Targeting Rules ---

    public List<AdTargetingRuleResponse> getRules(UUID adId) {
        return ruleRepository.findByAdId(adId).stream()
                .map(this::mapToRuleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdTargetingRuleResponse addRule(UUID adId, AdTargetingRuleRequest request) {
        Ad ad = findAd(adId);
        AdTargetingRule rule = new AdTargetingRule();
        rule.setAd(ad);
        rule.setMinAge(request.minAge());
        rule.setMaxAge(request.maxAge());
        rule.setTargetState(request.targetState());
        rule.setTargetGender(request.targetGender());
        rule.setAdCategory(request.adCategory());
        return mapToRuleResponse(ruleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    // --- Impressions ---

    @Transactional
    public void recordImpression(Long userProfileId, UUID adId) {
        Ad ad = findAd(adId);
        UserProfile user = null;
        if (userProfileId != null) {
            user = userProfileRepository.findById(userProfileId).orElse(null);
        }

        AdImpression impression = new AdImpression();
        impression.setAd(ad);
        impression.setUserProfile(user);
        impressionRepository.save(impression);
    }

    // --- Ad Serving ---

    @Transactional
    public AdResponse serveAd(Long userProfileId) {
        UserProfile user = userProfileRepository.findById(userProfileId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Only show ads to FREE users
        if (user.getSubscriptionType() != SubscriptionType.FREE) {
            return null;
        }

        List<Ad> activeAds = adRepository.findByIsActiveTrue();
        List<Ad> eligibleAds = new ArrayList<>();

        for (Ad ad : activeAds) {
            List<AdTargetingRule> rules = ruleRepository.findByAdId(ad.getId());
            if (isEligible(user, rules)) {
                eligibleAds.add(ad);
            }
        }

        if (eligibleAds.isEmpty()) {
            return null; // No ad available
        }

        // Weighted random selection: Lower priority value = Higher frequency (1 > 99)
        // Formula: Weight = 1000.0 / (Priority + 1)
        // Prio 0 -> Weight 1000
        // Prio 1 -> Weight 500
        // Prio 99 -> Weight 10 (Prio 1 shows 50x more often than Prio 99)

        double totalWeight = 0;
        for (Ad ad : eligibleAds) {
            totalWeight += getAdWeight(ad);
        }

        double randomValue = random.nextDouble() * totalWeight;
        Ad selectedAd = null;
        for (Ad ad : eligibleAds) {
            randomValue -= getAdWeight(ad);
            if (randomValue <= 0) {
                selectedAd = ad;
                break;
            }
        }

        // Fallback (should theoretically not happen if totalWeight > 0)
        if (selectedAd == null) {
            selectedAd = eligibleAds.get(eligibleAds.size() - 1);
        }

        // Record impression
        recordImpression(userProfileId, selectedAd.getId());

        return mapToAdResponse(selectedAd);
    }

    private double getAdWeight(Ad ad) {
        // Ensure non-negative; treat 0 as highest priority
        int prio = Math.max(0, ad.getPriority());
        return 1000.0 / (prio + 1.0);
    }

    private boolean isEligible(UserProfile user, List<AdTargetingRule> rules) {
        if (rules.isEmpty()) {
            return true; // No rules, valid for everyone
        }

        // Must match ALL non-null criteria in AT LEAST ONE rule?
        // Usually, multiple rules for an Ad are OR conditions (e.g. Target CA OR Target
        // NY).
        // Let's assume rules are alternative criteria sets.
        // If an ad has 2 rules, either rule being satisfied makes it eligible.

        for (AdTargetingRule rule : rules) {
            if (matchesRule(user, rule)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRule(UserProfile user, AdTargetingRule rule) {
        // Age check
        if (rule.getMinAge() != null && (user.getAge() == null || user.getAge() < rule.getMinAge())) {
            return false;
        }
        if (rule.getMaxAge() != null && (user.getAge() == null || user.getAge() > rule.getMaxAge())) {
            return false;
        }

        // State check
        if (rule.getTargetState() != null
                && (user.getState() == null || !rule.getTargetState().equalsIgnoreCase(user.getState()))) {
            return false;
        }

        // Gender check
        if (rule.getTargetGender() != null
                && (user.getGender() == null || !rule.getTargetGender().equalsIgnoreCase(user.getGender()))) {
            return false;
        }

        return true;
    }

    // --- Helpers ---

    private Ad findAd(UUID id) {
        return adRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ad not found"));
    }

    private void updateAdFromRequest(Ad ad, AdRequest request) {
        ad.setName(request.name());
        ad.setAdType(request.adType());
        ad.setImageUrl(request.imageUrl());
        ad.setVideoUrl(request.videoUrl());
        ad.setTextContent(request.textContent());
        ad.setCtaText(request.ctaText());
        ad.setCtaUrl(request.ctaUrl());
        if (request.isActive()) // primitive, implies non-null in record if not wrapper
            ad.setActive(request.isActive());
        if (request.priority() != 0) // assumption: 0 is default/no-change, or we should use Integer in record
            ad.setPriority(request.priority());
        if (request.durationSeconds() != null)
            ad.setDurationSeconds(request.durationSeconds());
    }

    private AdResponse mapToAdResponse(Ad ad) {
        return new AdResponse(
                ad.getId(),
                ad.getName(),
                ad.getAdType(),
                ad.getImageUrl(),
                ad.getVideoUrl(),
                ad.getTextContent(),
                ad.getCtaText(),
                ad.getCtaUrl(),
                ad.getDurationSeconds(),
                ad.isActive(),
                ad.getPriority(),
                ad.getCreatedAt(),
                ad.getUpdatedAt());
    }

    private AdTargetingRuleResponse mapToRuleResponse(AdTargetingRule rule) {
        return new AdTargetingRuleResponse(
                rule.getId(),
                rule.getAd().getId(),
                rule.getMinAge(),
                rule.getMaxAge(),
                rule.getTargetState(),
                rule.getTargetGender(),
                rule.getAdCategory(),
                rule.getCreatedAt());
    }
}
