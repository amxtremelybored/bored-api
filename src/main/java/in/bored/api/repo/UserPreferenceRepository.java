// src/main/java/in/bored/api/repo/UserPreferenceRepository.java
package in.bored.api.repo;

import in.bored.api.model.ContentCategory;
import in.bored.api.model.UserPreference;
import in.bored.api.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    List<UserPreference> findByUserProfile(UserProfile userProfile);

    Optional<UserPreference> findByUserProfileAndCategory(UserProfile userProfile,
                                                          ContentCategory category);
    void deleteByUserProfile(UserProfile profile);
}