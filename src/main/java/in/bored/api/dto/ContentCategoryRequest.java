// src/main/java/in/bored/api/dto/ContentCategoryRequest.java
package in.bored.api.dto;

import java.time.OffsetDateTime;

public class ContentCategoryRequest {

    private String name;
    private String emoji;
    private Boolean contentLoaded;
    private OffsetDateTime contentLoadedAt;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public Boolean getContentLoaded() { return contentLoaded; }
    public void setContentLoaded(Boolean contentLoaded) { this.contentLoaded = contentLoaded; }

    public OffsetDateTime getContentLoadedAt() { return contentLoadedAt; }
    public void setContentLoadedAt(OffsetDateTime contentLoadedAt) { this.contentLoadedAt = contentLoadedAt; }
}