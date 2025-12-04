package in.bored.api.repo;

import in.bored.api.model.FunCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FunCategoryRepository extends JpaRepository<FunCategory, UUID> {
    Optional<FunCategory> findByName(String name);
}
