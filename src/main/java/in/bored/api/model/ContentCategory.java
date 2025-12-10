// src/main/java/in/bored/api/model/ContentCategory.java
package in.bored.api.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "content_category")
public class ContentCategory {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "emoji", length = 16)
    private String emoji;

    @Column(name = "is_content_loaded", nullable = false)
    private boolean contentLoaded = false;

    @Column(name = "content_loaded_at")
    private OffsetDateTime contentLoadedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonBackReference
    private SuperCategory superCategory;

    public ContentCategory() {
    }

    public ContentCategory(UUID id,
            String name,
            String emoji,
            boolean contentLoaded,
            OffsetDateTime contentLoadedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.emoji = emoji;
        this.contentLoaded = contentLoaded;
        this.contentLoadedAt = contentLoadedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // getters/setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public boolean isContentLoaded() {
        return contentLoaded;
    }

    public void setContentLoaded(boolean contentLoaded) {
        this.contentLoaded = contentLoaded;
    }

    public OffsetDateTime getContentLoadedAt() {
        return contentLoadedAt;
    }

    public void setContentLoadedAt(OffsetDateTime contentLoadedAt) {
        this.contentLoadedAt = contentLoadedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public SuperCategory getSuperCategory() {
        return superCategory;
    }

    public void setSuperCategory(SuperCategory superCategory) {
        this.superCategory = superCategory;
    }
}