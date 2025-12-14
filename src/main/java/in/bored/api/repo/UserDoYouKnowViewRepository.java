package in.bored.api.repo;

import in.bored.api.model.UserDoYouKnowView;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDoYouKnowViewRepository extends JpaRepository<UserDoYouKnowView, Long> {
    Optional<UserDoYouKnowView> findByUserProfileIdAndDoYouKnowContentId(Long userProfileId, Long doYouKnowContentId);

    boolean existsByUserProfileIdAndDoYouKnowContentId(Long userProfileId, Long doYouKnowContentId);
}
