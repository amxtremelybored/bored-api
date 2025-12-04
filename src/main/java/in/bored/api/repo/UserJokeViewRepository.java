package in.bored.api.repo;

import in.bored.api.model.UserJokeView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJokeViewRepository extends JpaRepository<UserJokeView, Long> {
}
