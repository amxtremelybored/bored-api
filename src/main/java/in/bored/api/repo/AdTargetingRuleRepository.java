package in.bored.api.repo;

import in.bored.api.model.AdTargetingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface AdTargetingRuleRepository extends JpaRepository<AdTargetingRule, UUID> {
    List<AdTargetingRule> findByAdId(UUID adId);
}
