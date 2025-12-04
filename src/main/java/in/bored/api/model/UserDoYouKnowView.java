package in.bored.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_doyouknow_view", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_profile_id", "doyouknow_content_id" })
})
public class UserDoYouKnowView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_profile_id")
    private Long userProfileId;

    @Column(name = "doyouknow_content_id")
    private Long doYouKnowContentId;

    @CreationTimestamp
    @Column(name = "viewed_at", nullable = false, updatable = false)
    private OffsetDateTime viewedAt;

    @Column(name = "is_liked")
    private Boolean isLiked;

    public UserDoYouKnowView() {
    }

    public UserDoYouKnowView(Long userProfileId, Long doYouKnowContentId, Boolean isLiked) {
        this.userProfileId = userProfileId;
        this.doYouKnowContentId = doYouKnowContentId;
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

    public Long getDoYouKnowContentId() {
        return doYouKnowContentId;
    }

    public void setDoYouKnowContentId(Long doYouKnowContentId) {
        this.doYouKnowContentId = doYouKnowContentId;
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
