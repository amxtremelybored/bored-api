// src/main/java/in/bored/api/dto/TopicSummary.java
package in.bored.api.dto;

import java.util.UUID;

public class TopicSummary {

    private Long id;          // topic id (matches Topic.id & ContentItemResponse.topicId)
    private String name;
    private String emoji;
    private UUID categoryId;  // category id (matches ContentCategory.id)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }
}