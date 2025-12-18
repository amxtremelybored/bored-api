package in.bored.api.repo;

import in.bored.api.model.UserHealthWellnessView;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserHealthWellnessViewRepository extends ListCrudRepository<UserHealthWellnessView, Long> {
    boolean existsByUserProfileIdAndHealthWellnessContentId(Long userProfileId, Long healthWellnessContentId);
}
