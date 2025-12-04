package in.bored.api.repo;

import in.bored.api.model.PuzzleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PuzzleCategoryRepository extends JpaRepository<PuzzleCategory, UUID> {
}
