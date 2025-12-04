package in.bored.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_fun_view", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_profile_id", "fun_content_id" })
})
public class UserFunView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_profile_id")
    private Long userProfileId;

    @Column(name = "fun_content_id")
    private Long funContentId;

    @CreationTimestamp
    @Column(name = "viewed_at", nullable = false, updatable = false)
    private OffsetDateTime viewedAt;

    @Column(name = "is_liked")
    private Boolean isLiked;

    public UserFunView() {
    }

    public UserFunView(Long userProfileId, Long funContentId, Boolean isLiked) {
        this.userProfileId = userProfileId;
        this.funContentId = funContentId;
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

    public Long getFunContentId() {
        return funContentId;
    }

    public void setFunContentId(Long funContentId) {
        this.funContentId = funContentId;
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
