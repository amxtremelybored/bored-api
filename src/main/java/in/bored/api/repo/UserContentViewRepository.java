// src/main/java/in/bored/api/repo/UserContentViewRepository.java
package in.bored.api.repo;

import in.bored.api.model.TopicContent;
import in.bored.api.model.UserContentView;
import in.bored.api.model.UserProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserContentViewRepository extends JpaRepository<UserContentView, Long> {

    @Query("SELECT v.topic.id FROM UserContentView v WHERE v.userProfile = :profile ORDER BY v.viewedAt DESC")
    List<Long> findRecentTopicIds(@Param("profile") UserProfile profile, Pageable pageable);

    @Query("SELECT v.topic.id FROM UserContentView v WHERE v.guestUid = :guestUid ORDER BY v.viewedAt DESC")
    List<Long> findRecentTopicIdsForGuest(@Param("guestUid") String guestUid, Pageable pageable);

    @Query("SELECT v.topicContent FROM UserContentView v WHERE v.userProfile = :profile AND v.saved = true ORDER BY v.viewedAt DESC")
    List<TopicContent> findSavedContentForUser(@Param("profile") UserProfile profile, Pageable pageable);

    @Query("SELECT v.topicContent FROM UserContentView v WHERE v.guestUid = :guestUid AND v.saved = true ORDER BY v.viewedAt DESC")
    List<TopicContent> findSavedContentForGuest(@Param("guestUid") String guestUid, Pageable pageable);

    java.util.Optional<UserContentView> findByUserProfileAndTopicContent_Id(UserProfile userProfile,
            Long topicContentId);

    java.util.Optional<UserContentView> findByGuestUidAndTopicContent_Id(String guestUid, Long topicContentId);

    void deleteByTopic(in.bored.api.model.Topic topic);
}