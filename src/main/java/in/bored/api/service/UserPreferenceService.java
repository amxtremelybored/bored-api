// src/main/java/in/bored/api/service/UserPreferenceService.java
package in.bored.api.service;

import in.bored.api.dto.UserPreferenceBulkRequest;
import in.bored.api.dto.UserPreferenceRequest;
import in.bored.api.model.ProfileStatus;
import in.bored.api.model.Topic;
import in.bored.api.model.UserPreference;
import in.bored.api.model.UserProfile;
import in.bored.api.repo.TopicRepository;
import in.bored.api.repo.UserPreferenceRepository;
import in.bored.api.repo.UserProfileRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserProfileRepository userProfileRepository;
    private final TopicRepository topicRepository;

    public UserPreferenceService(UserPreferenceRepository userPreferenceRepository,
                                 UserProfileRepository userProfileRepository,
                                 TopicRepository topicRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userProfileRepository = userProfileRepository;
        this.topicRepository = topicRepository;
    }

    // Create single preference for CURRENT user
    public UserPreference create(UserPreferenceRequest request) {
        UserProfile profile = getCurrentUserProfile();

        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + request.getTopicId()));

        userPreferenceRepository.findByUserProfileAndTopic(profile, topic)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Preference already exists for this user and topic");
                });

        UserPreference pref = new UserPreference();
        pref.setUserProfile(profile);
        pref.setTopic(topic);

        return userPreferenceRepository.save(pref);
    }

    public UserPreference getById(Long id) {
        return userPreferenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserPreference not found: " + id));
    }

    // Get all preferences for CURRENT user
    public List<UserPreference> getCurrentUserPreferences() {
        UserProfile profile = getCurrentUserProfile();
        return userPreferenceRepository.findByUserProfile(profile);
    }

    public void delete(Long id) {
        UserPreference pref = getById(id);
        userPreferenceRepository.delete(pref);
    }

    // Bulk add multiple topics for CURRENT user
    public List<UserPreference> addPreferencesForCurrentUser(List<Long> topicIds) {
        UserProfile profile = getCurrentUserProfile();

        List<Topic> topics = topicRepository.findAllById(topicIds);
        if (topics.size() != topicIds.size()) {
            throw new ResourceNotFoundException("One or more topicIds are invalid");
        }

        List<UserPreference> existing = userPreferenceRepository.findByUserProfile(profile);
        Set<Long> existingTopicIds = existing.stream()
                .map(p -> p.getTopic().getId())
                .collect(Collectors.toSet());

        List<UserPreference> newPrefs = topics.stream()
                .filter(t -> !existingTopicIds.contains(t.getId()))
                .map(t -> {
                    UserPreference p = new UserPreference();
                    p.setUserProfile(profile);
                    p.setTopic(t);
                    return p;
                })
                .toList();

        return userPreferenceRepository.saveAll(newPrefs);
    }

    // === helpers ===

    private UserProfile getCurrentUserProfile() {
        String uid = getCurrentUid();
        return userProfileRepository.findByUidAndStatusNot(uid, ProfileStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found or deleted for uid: " + uid));
    }

    private String getCurrentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return auth.getPrincipal().toString();
    }
}