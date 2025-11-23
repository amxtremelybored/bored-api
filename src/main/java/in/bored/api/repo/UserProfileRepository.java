// src/main/java/in/bored/api/repo/UserProfileRepository.java
package in.bored.api.repo;

import in.bored.api.model.ProfileStatus;
import in.bored.api.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Page<UserProfile> findByStatusNot(ProfileStatus status, Pageable pageable);

    Optional<UserProfile> findByIdAndStatusNot(UUID id, ProfileStatus status);

    Optional<UserProfile> findByUidAndStatusNot(String uid, ProfileStatus status);
}