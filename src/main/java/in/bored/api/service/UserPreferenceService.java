// src/main/java/in/bored/api/service/UserPreferenceService.java
package in.bored.api.service;

import in.bored.api.dto.UserPreferenceBulkRequest;
import in.bored.api.dto.UserPreferenceRequest;
import in.bored.api.model.ContentCategory;
import in.bored.api.model.ProfileStatus;
import in.bored.api.model.UserPreference;
import in.bored.api.model.UserProfile;
import in.bored.api.repo.ContentCategoryRepository;
import in.bored.api.repo.UserPreferenceRepository;
import in.bored.api.repo.UserProfileRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserProfileRepository userProfileRepository;
    private final ContentCategoryRepository contentCategoryRepository;

    public UserPreferenceService(UserPreferenceRepository userPreferenceRepository,
                                 UserProfileRepository userProfileRepository,
                                 ContentCategoryRepository contentCategoryRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userProfileRepository = userProfileRepository;
        this.contentCategoryRepository = contentCategoryRepository;
    }

    public UserPreference create(UserPreferenceRequest request) {
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("categoryId is required");
        }

        UserProfile profile = getCurrentUserProfile();

        UUID categoryId = request.getCategoryId();
        ContentCategory category = contentCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentCategory not found: " + categoryId));

        userPreferenceRepository.findByUserProfileAndCategory(profile, category)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Preference already exists for this user and category");
                });

        UserPreference pref = new UserPreference();
        pref.setUserProfile(profile);
        pref.setCategory(category);

        return userPreferenceRepository.save(pref);
    }

    public UserPreference getById(Long id) {
        return userPreferenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserPreference not found: " + id));
    }

    public List<UserPreference> getCurrentUserPreferences() {
        UserProfile profile = getCurrentUserProfile();
        return userPreferenceRepository.findByUserProfile(profile);
    }

    public void delete(Long id) {
        UserPreference pref = getById(id);
        userPreferenceRepository.delete(pref);
    }

    public List<UserPreference> addPreferencesForCurrentUser(List<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException("categoryIds must not be empty");
        }
        if (categoryIds.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("categoryIds must not contain null");
        }

        UserProfile profile = getCurrentUserProfile();

        List<ContentCategory> categories = contentCategoryRepository.findAllById(categoryIds);
        if (categories.size() != new HashSet<>(categoryIds).size()) {
            throw new ResourceNotFoundException("One or more categoryIds are invalid");
        }

        List<UserPreference> existing = userPreferenceRepository.findByUserProfile(profile);
        Set<UUID> existingCategoryIds = existing.stream()
                .map(p -> p.getCategory().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<UserPreference> newPrefs = categories.stream()
                .filter(c -> !existingCategoryIds.contains(c.getId()))
                .map(c -> {
                    UserPreference p = new UserPreference();
                    p.setUserProfile(profile);
                    p.setCategory(c);
                    return p;
                })
                .toList();

        return userPreferenceRepository.saveAll(newPrefs);
    }

    // === helpers ===

    private UserProfile getCurrentUserProfile() {
        String uid = getCurrentUid();
        return userProfileRepository.findByUidAndStatusNot(uid, ProfileStatus.DELETED)
                .orElseThrow(() ->
                        new ResourceNotFoundException("UserProfile not found or deleted for uid: " + uid));
    }

    private String getCurrentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return auth.getPrincipal().toString();
    }
}