package in.bored.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_joke_view", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_profile_id", "joke_content_id" })
})
public class UserJokeView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_profile_id")
    private Long userProfileId;

    @Column(name = "joke_content_id")
    private Long jokeContentId;

    @CreationTimestamp
    @Column(name = "viewed_at", nullable = false, updatable = false)
    private OffsetDateTime viewedAt;

    @Column(name = "is_liked")
    private Boolean isLiked;

    public UserJokeView() {
    }

    public UserJokeView(Long userProfileId, Long jokeContentId, Boolean isLiked) {
        this.userProfileId = userProfileId;
        this.jokeContentId = jokeContentId;
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

    public Long getJokeContentId() {
        return jokeContentId;
    }

    public void setJokeContentId(Long jokeContentId) {
        this.jokeContentId = jokeContentId;
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
