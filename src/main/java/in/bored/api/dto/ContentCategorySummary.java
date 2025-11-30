// src/main/java/in/bored/api/dto/ContentCategorySummary.java
package in.bored.api.dto;

import java.util.UUID;

public class ContentCategorySummary {

    private UUID id;
    private String name;
    private String emoji;

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
}