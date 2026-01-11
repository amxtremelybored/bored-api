package in.bored.api.repo;

import in.bored.api.model.BulkAdItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface BulkAdItemRepository extends JpaRepository<BulkAdItem, UUID> {
    List<BulkAdItem> findByIsActiveTrueOrderBySortOrderAsc();
}
