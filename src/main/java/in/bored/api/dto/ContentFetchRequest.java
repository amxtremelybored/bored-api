// src/main/java/in/bored/api/dto/ContentFetchRequest.java
package in.bored.api.dto;

import java.util.List;

public class ContentFetchRequest {

    // Optional: if null/empty -> use user's preferences
    private List<Long> topicIds;

    // How many content items to fetch (default in service if null)
    private Integer size;

    public List<Long> getTopicIds() { return topicIds; }
    public void setTopicIds(List<Long> topicIds) { this.topicIds = topicIds; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}