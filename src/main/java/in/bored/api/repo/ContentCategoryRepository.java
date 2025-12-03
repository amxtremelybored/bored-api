// src/main/java/in/bored/api/repo/ContentCategoryRepository.java
package in.bored.api.repo;

import in.bored.api.model.ContentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContentCategoryRepository extends JpaRepository<ContentCategory, UUID> {
    boolean existsByNameIgnoreCase(String name);

    Optional<ContentCategory> findByNameIgnoreCase(String name);

    java.util.List<ContentCategory> findAllByContentLoadedTrue();
}