// src/main/java/in/bored/api/model/Topic.java
package in.bored.api.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String emoji;

    @Column(name = "is_content_loaded")
    private boolean contentLoaded;

    @Column(name = "content_loaded_at")
    private OffsetDateTime contentLoadedAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public boolean isContentLoaded() { return contentLoaded; }
    public void setContentLoaded(boolean contentLoaded) { this.contentLoaded = contentLoaded; }

    public OffsetDateTime getContentLoadedAt() { return contentLoadedAt; }
    public void setContentLoadedAt(OffsetDateTime contentLoadedAt) { this.contentLoadedAt = contentLoadedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}