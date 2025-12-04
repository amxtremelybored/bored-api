// src/main/java/in/bored/api/dto/ContentItemResponse.java
package in.bored.api.dto;

import java.time.OffsetDateTime;

public class ContentItemResponse {

    private Long id;
    private Long topicId;
    private String topicName;
    private String topicEmoji;
    private Integer contentIndex;
    private String content;
    private OffsetDateTime createdAt;
    private String source;
    private String categoryName;
    private String topicDisplayName;

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicEmoji() {
        return topicEmoji;
    }

    public void setTopicEmoji(String topicEmoji) {
        this.topicEmoji = topicEmoji;
    }

    public Integer getContentIndex() {
        return contentIndex;
    }

    public void setContentIndex(Integer contentIndex) {
        this.contentIndex = contentIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getTopicDisplayName() {
        return topicDisplayName;
    }

    public void setTopicDisplayName(String topicDisplayName) {
        this.topicDisplayName = topicDisplayName;
    }
}