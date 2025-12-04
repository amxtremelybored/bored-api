package in.bored.api.repo;

import in.bored.api.model.DoYouKnowCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoYouKnowCategoryRepository extends JpaRepository<DoYouKnowCategory, UUID> {
    Optional<DoYouKnowCategory> findByName(String name);
}
