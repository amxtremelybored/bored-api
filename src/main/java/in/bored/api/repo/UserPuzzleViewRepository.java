package in.bored.api.repo;

import in.bored.api.model.UserPuzzleView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPuzzleViewRepository extends JpaRepository<UserPuzzleView, Long> {
}
