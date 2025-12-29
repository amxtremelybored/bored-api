package in.bored.api.repo;

import in.bored.api.model.AdImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdImpressionRepository extends JpaRepository<AdImpression, Long> {
}
