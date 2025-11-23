// src/main/java/in/bored/api/model/UserContentView.java
package in.bored.api.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "user_content_views",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_profile_id", "topic_content_id"})
)
public class UserContentView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    // content
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_content_id", nullable = false)
    private TopicContent topicContent;

    // direct topic reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "viewed_at", updatable = false)
    private OffsetDateTime viewedAt;

    @PrePersist
    public void prePersist() {
        this.viewedAt = OffsetDateTime.now();
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserProfile getUserProfile() { return userProfile; }
    public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }

    public TopicContent getTopicContent() { return topicContent; }
    public void setTopicContent(TopicContent topicContent) { this.topicContent = topicContent; }

    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }

    public OffsetDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(OffsetDateTime viewedAt) { this.viewedAt = viewedAt; }
}