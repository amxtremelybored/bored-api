package in.bored.api.repo;

import in.bored.api.model.Topic;
import in.bored.api.model.UserProfile;
import in.bored.api.model.UserTopicBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTopicBookmarkRepository extends JpaRepository<UserTopicBookmark, Long> {

    Optional<UserTopicBookmark> findByUserProfileAndTopic(UserProfile userProfile, Topic topic);

    Optional<UserTopicBookmark> findByGuestUidAndTopic(String guestUid, Topic topic);

    List<UserTopicBookmark> findByUserProfile(UserProfile userProfile);

    List<UserTopicBookmark> findByGuestUid(String guestUid);
}
