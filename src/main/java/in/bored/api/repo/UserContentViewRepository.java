// src/main/java/in/bored/api/repo/UserContentViewRepository.java
package in.bored.api.repo;

import in.bored.api.model.UserContentView;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserContentViewRepository extends JpaRepository<UserContentView, Long> {
}