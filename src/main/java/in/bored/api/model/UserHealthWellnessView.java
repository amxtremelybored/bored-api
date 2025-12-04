package in.bored.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_health_wellness_view", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_profile_id", "health_wellness_content_id" })
})
public class UserHealthWellnessView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_profile_id")
    private Long userProfileId;

    @Column(name = "health_wellness_content_id")
    private Long healthWellnessContentId;

    @CreationTimestamp
    @Column(name = "viewed_at", nullable = false, updatable = false)
    private OffsetDateTime viewedAt;

    @Column(name = "is_liked")
    private Boolean isLiked;

    public UserHealthWellnessView() {
    }

    public UserHealthWellnessView(Long userProfileId, Long healthWellnessContentId, Boolean isLiked) {
        this.userProfileId = userProfileId;
        this.healthWellnessContentId = healthWellnessContentId;
        this.isLiked = isLiked;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserProfileId() {
        return userProfileId;
    }

    public void setUserProfileId(Long userProfileId) {
        this.userProfileId = userProfileId;
    }

    public Long getHealthWellnessContentId() {
        return healthWellnessContentId;
    }

    public void setHealthWellnessContentId(Long healthWellnessContentId) {
        this.healthWellnessContentId = healthWellnessContentId;
    }

    public OffsetDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(OffsetDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }

    public Boolean getIsLiked() {
        return isLiked;
    }

    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }
}
