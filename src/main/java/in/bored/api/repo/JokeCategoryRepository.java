package in.bored.api.repo;

import in.bored.api.model.JokeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JokeCategoryRepository extends JpaRepository<JokeCategory, UUID> {
    Optional<JokeCategory> findByName(String name);
}
