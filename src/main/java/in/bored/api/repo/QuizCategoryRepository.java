package in.bored.api.repo;

import in.bored.api.model.QuizCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuizCategoryRepository extends JpaRepository<QuizCategory, UUID> {
}
