// src/main/java/in/bored/api/dto/ContentCategoryResponse.java
package in.bored.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ContentCategoryResponse {

    private UUID id;
    private String name;
    private String emoji;
    private boolean contentLoaded;
    private OffsetDateTime contentLoadedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ContentCategoryResponse(UUID id,
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

    // getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmoji() { return emoji; }
    public boolean isContentLoaded() { return contentLoaded; }
    public OffsetDateTime getContentLoadedAt() { return contentLoadedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}