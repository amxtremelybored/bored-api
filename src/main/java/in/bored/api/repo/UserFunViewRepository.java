package in.bored.api.repo;

import in.bored.api.model.UserFunView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFunViewRepository extends JpaRepository<UserFunView, Long> {

    boolean existsByUserProfileAndFunContent(in.bored.api.model.UserProfile userProfile,
            in.bored.api.model.FunContent funContent);
}
