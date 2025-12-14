package in.bored.api.repo;

import in.bored.api.model.UserQuizView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserQuizViewRepository extends JpaRepository<UserQuizView, Long> {

    boolean existsByUserProfileAndQuizContent(in.bored.api.model.UserProfile userProfile,
            in.bored.api.model.QuizContent quizContent);
}
