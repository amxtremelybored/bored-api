// src/main/java/in/bored/api/model/UserPreference.java
package in.bored.api.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "user_preferences",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_profile_id", "topic_id"})
)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // simple numeric PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UserProfile getUserProfile() { return userProfile; }
    public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }

    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}