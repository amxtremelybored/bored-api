// src/main/java/in/bored/api/service/UserProfileService.java
package in.bored.api.service;

import in.bored.api.dto.UserProfileRequest;
import in.bored.api.model.ProfileStatus;
import in.bored.api.model.UserProfile;
import in.bored.api.repo.UserProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile create(UserProfileRequest request) {
        UserProfile profile = new UserProfile();
        profile.setId(UUID.randomUUID());
        applyRequest(profile, request);
        return userProfileRepository.save(profile);
    }

    public UserProfile getById(UUID id) {
        return userProfileRepository.findByIdAndStatusNot(id, ProfileStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found: " + id));
    }

    public Page<UserProfile> list(Pageable pageable) {
        return userProfileRepository.findByStatusNot(ProfileStatus.DELETED, pageable);
    }

    public UserProfile update(UUID id, UserProfileRequest request) {
        UserProfile profile = getById(id);
        applyRequest(profile, request);
        return userProfileRepository.save(profile);
    }

    public void softDelete(UUID id) {
        UserProfile profile = getById(id);
        profile.setStatus(ProfileStatus.DELETED);
        userProfileRepository.save(profile);
    }

    public UserProfile getByUid(String uid) {
        return userProfileRepository.findByUidAndStatusNot(uid, ProfileStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found for uid: " + uid));
    }

    public UserProfile getCurrentUserProfile() {
        String uid = getCurrentUid();
        return getByUid(uid);
    }

    public UserProfile upsertCurrentUserProfile(UserProfileRequest request) {
        String firebaseUid = getCurrentUid();

        UserProfile profile = userProfileRepository
                .findByUidAndStatusNot(firebaseUid, ProfileStatus.DELETED)
                .orElseGet(() -> {
                    UserProfile p = new UserProfile();
                    p.setId(UUID.randomUUID());
                    p.setUid(firebaseUid);
                    p.setFirebaseUid(firebaseUid);
                    p.setStatus(ProfileStatus.ACTIVE);
                    return p;
                });

        applyRequest(profile, request);
        return userProfileRepository.save(profile);
    }

    private String getCurrentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return auth.getPrincipal().toString();
    }

    private void applyRequest(UserProfile profile, UserProfileRequest request) {
        if (request.getDisplayName() != null) {
            profile.setDisplayName(request.getDisplayName());
        }
        if (request.getAge() != null) {
            profile.setAge(request.getAge());
        }
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail());
        }
        if (request.getPhotoUrl() != null) {
            profile.setPhotoUrl(request.getPhotoUrl());
        }
    }
}