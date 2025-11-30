// src/main/java/in/bored/api/dto/ContentEmptyMetaResponse.java
package in.bored.api.dto;

import java.util.List;

public class ContentEmptyMetaResponse {

    private boolean empty;
    private List<ContentCategorySummary> preferredCategories;
    private List<TopicSummary> suggestedTopics;

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public List<ContentCategorySummary> getPreferredCategories() {
        return preferredCategories;
    }

    public void setPreferredCategories(List<ContentCategorySummary> preferredCategories) {
        this.preferredCategories = preferredCategories;
    }

    public List<TopicSummary> getSuggestedTopics() {
        return suggestedTopics;
    }

    public void setSuggestedTopics(List<TopicSummary> suggestedTopics) {
        this.suggestedTopics = suggestedTopics;
    }
}