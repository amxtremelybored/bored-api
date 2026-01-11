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
import java.time.OffsetDateTime;

@Service
public class AdService {

    private final AdRepository adRepository;
    private final AdTargetingRuleRepository ruleRepository;
    private final AdImpressionRepository impressionRepository;
    private final UserProfileRepository userProfileRepository;
    private final BulkAdItemRepository bulkAdItemRepository;
    private final Random random = new Random();

    public AdService(AdRepository adRepository,
            AdTargetingRuleRepository ruleRepository,
            AdImpressionRepository impressionRepository,
            UserProfileRepository userProfileRepository,
            BulkAdItemRepository bulkAdItemRepository) {
        this.adRepository = adRepository;
        this.ruleRepository = ruleRepository;
        this.impressionRepository = impressionRepository;
        this.userProfileRepository = userProfileRepository;
        this.bulkAdItemRepository = bulkAdItemRepository;
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
        return serveAdInternal(user);
    }

    private AdResponse serveAdInternal(UserProfile user) {
        // Only show ads to FREE users
        if (user.getSubscriptionType() != SubscriptionType.FREE) {
            return null;
        }

        // Check Cooldown from Bulk Ads
        OffsetDateTime lastShown = user.getLastBulkAdShownTime();
        if (lastShown != null) {
            java.time.Duration timeSince = java.time.Duration.between(lastShown, java.time.OffsetDateTime.now());
            if (timeSince.toMinutes() < 60) {
                // Cooldown active: Do NOT show regular ads either
                return null;
            }
        }

        List<Ad> activeAds = adRepository.findByIsActiveTrue();
        List<Ad> eligibleAds = filterEligibleAds(user, activeAds);

        if (eligibleAds.isEmpty()) {
            return null; // No ad available
        }

        // Strict Priority Logic:
        // 1. Find the best (lowest value) priority among eligible ads.
        int bestPriority = eligibleAds.stream()
                .mapToInt(Ad::getPriority)
                .min()
                .orElse(Integer.MAX_VALUE);

        // 2. Filter to only get ads with that priority
        List<Ad> bestAds = eligibleAds.stream()
                .filter(ad -> ad.getPriority() == bestPriority)
                .collect(Collectors.toList());

        // 3. Split equally (Random Uniform Selection)
        Ad selectedAd = bestAds.get(random.nextInt(bestAds.size()));

        recordImpression(user.getId(), selectedAd.getId());
        return mapToAdResponse(selectedAd);
    }

    @Transactional
    public List<AdResponse> serveBulkAds(Long userProfileId) {
        System.out.println("DEBUG: serveBulkAds called for user: " + userProfileId);
        UserProfile user = userProfileRepository.findById(userProfileId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getSubscriptionType() != SubscriptionType.FREE) {
            System.out.println("DEBUG: User is not FREE, returning empty list");
            return new ArrayList<>();
        }

        // Check Cooldown
        OffsetDateTime lastShown = user.getLastBulkAdShownTime();
        if (lastShown != null) {
            java.time.Duration timeSince = java.time.Duration.between(lastShown, java.time.OffsetDateTime.now());
            System.out.println(
                    "DEBUG: Last bulk ad shown at: " + lastShown + ", Minutes since: " + timeSince.toMinutes());
            if (timeSince.toMinutes() < 60) {
                System.out.println("DEBUG: Cooldown ACTIVE. Returning empty list.");
                return new ArrayList<>();
            }
        } else {
            System.out.println("DEBUG: First time bulk ad for this user (lastShown is null)");
        }

        // Serve Bulk
        List<BulkAdItem> bulkItems = bulkAdItemRepository.findByIsActiveTrueOrderBySortOrderAsc();
        System.out.println("DEBUG: Found " + bulkItems.size() + " active bulk items configured.");

        if (bulkItems.isEmpty()) {
            System.out.println("DEBUG: No bulk items found in DB. Returning empty list.");
            return new ArrayList<>();
        }

        List<AdResponse> result = new ArrayList<>();
        java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Kolkata"));

        for (BulkAdItem item : bulkItems) {
            Ad ad = item.getAd();
            if (!ad.isActive()) {
                System.out.println("DEBUG: Ad " + ad.getId() + " is inactive, skipping.");
                continue;
            }

            // Check Time Slots
            if (!isEligibleForTimeSlot(ad, now)) {
                System.out.println("DEBUG: Ad " + ad.getId() + " not eligible for time slot " + now);
                continue;
            }

            List<AdTargetingRule> rules = ruleRepository.findByAdId(ad.getId());
            if (isEligible(user, rules)) {
                System.out.println("DEBUG: Ad " + ad.getId() + " ELIGIBLE. Adding to result.");
                recordImpression(user.getId(), ad.getId());
                result.add(mapToAdResponse(ad));
            } else {
                System.out.println("DEBUG: Ad " + ad.getId() + " targeting mismatch.");
            }
        }

        if (result.isEmpty()) {
            System.out.println("DEBUG: All bulk ads filtered out. Returning empty list.");
            return new ArrayList<>();
        }

        // Update User Cooldown
        System.out.println("DEBUG: Successfully serving " + result.size() + " bulk ads. Updating lastBulkAdShownTime.");
        user.setLastBulkAdShownTime(java.time.OffsetDateTime.now());
        userProfileRepository.save(user);

        return result;
    }

    private List<Ad> filterEligibleAds(UserProfile user, List<Ad> sourceAds) {
        List<Ad> eligible = new ArrayList<>();
        java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Kolkata"));

        for (Ad ad : sourceAds) {
            if (!isEligibleForTimeSlot(ad, now))
                continue;
            List<AdTargetingRule> rules = ruleRepository.findByAdId(ad.getId());
            if (isEligible(user, rules)) {
                eligible.add(ad);
            }
        }
        return eligible;
    }

    private boolean isEligibleForTimeSlot(Ad ad, java.time.LocalTime now) {
        if (ad.getSlots() == null || ad.getSlots().isEmpty()) {
            return true; // No slots assigned -> Show 24/7
        }
        for (AdSlot slot : ad.getSlots()) {
            if (now.isAfter(slot.getStartTime()) && now.isBefore(slot.getEndTime())) {
                return true; // Current time is within a slot
            }
            // Handle cross-midnight slots if necessary (e.g. 23:00 to 02:00)
            // Assuming simple start < end for now as per requirement (9 to 12)
        }
        return false;
    }

    private boolean isEligible(UserProfile user, List<AdTargetingRule> rules) {
        if (rules.isEmpty())
            return true;
        for (AdTargetingRule rule : rules) {
            if (matchesRule(user, rule))
                return true;
        }
        return false;
    }

    private boolean matchesRule(UserProfile user, AdTargetingRule rule) {
        // Age check: Only fail if user age is KNOWN and out of range
        if (rule.getMinAge() != null && user.getAge() != null && user.getAge() < rule.getMinAge())
            return false;
        if (rule.getMaxAge() != null && user.getAge() != null && user.getAge() > rule.getMaxAge())
            return false;

        // State check: Only fail if user state is KNOWN and does not match
        if (rule.getTargetState() != null
                && user.getState() != null && !rule.getTargetState().equalsIgnoreCase(user.getState()))
            return false;

        // Gender check: Only fail if user gender is KNOWN and does not match
        if (rule.getTargetGender() != null
                && user.getGender() != null && !rule.getTargetGender().equalsIgnoreCase(user.getGender()))
            return false;

        return true;
    }

    private Ad findAd(UUID id) {
        return adRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Ad not found"));
    }

    @org.springframework.beans.factory.annotation.Autowired
    private AdSlotRepository adSlotRepository;

    private void updateAdFromRequest(Ad ad, AdRequest request) {
        ad.setName(request.name());
        ad.setAdType(request.adType());
        ad.setImageUrl(request.imageUrl());
        ad.setVideoUrl(request.videoUrl());
        ad.setTextContent(request.textContent());
        ad.setCtaText(request.ctaText());
        ad.setCtaUrl(request.ctaUrl());
        if (request.isActive())
            ad.setActive(request.isActive());
        if (request.priority() != 0)
            ad.setPriority(request.priority());
        if (request.durationSeconds() != null)
            ad.setDurationSeconds(request.durationSeconds());
        if (request.displayFormat() != null)
            ad.setDisplayFormat(request.displayFormat());

        if (request.slotIds() != null) {
            java.util.Set<AdSlot> slots = new java.util.HashSet<>(adSlotRepository.findAllById(request.slotIds()));
            ad.setSlots(slots);
        }
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
                ad.getDisplayFormat(),
                ad.getSlots().stream().map(AdSlot::getName).collect(Collectors.toList()),
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
