package in.bored.api.repo;

import in.bored.api.model.Ad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface AdRepository extends JpaRepository<Ad, UUID> {
    List<Ad> findByIsActiveTrue();
}
