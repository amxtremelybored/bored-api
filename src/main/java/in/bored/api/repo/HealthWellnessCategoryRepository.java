package in.bored.api.repo;

import in.bored.api.model.HealthWellnessCategory;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthWellnessCategoryRepository extends ListCrudRepository<HealthWellnessCategory, UUID> {
    Optional<HealthWellnessCategory> findByName(String name);
}
