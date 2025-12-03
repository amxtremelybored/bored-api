// src/main/java/in/bored/api/dto/ContentFetchRequest.java
package in.bored.api.dto;

import java.util.List;

public class ContentFetchRequest {

    private List<Long> topicIds;
    private Integer size;
    private Boolean refreshTopic;

    public List<Long> getTopicIds() {
        return topicIds;
    }

    public void setTopicIds(List<Long> topicIds) {
        this.topicIds = topicIds;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Boolean getRefreshTopic() {
        return refreshTopic;
    }

    public void setRefreshTopic(Boolean refreshTopic) {
        this.refreshTopic = refreshTopic;
    }
}