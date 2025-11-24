// src/main/java/in/bored/api/dto/GuestContentFetchRequest.java
package in.bored.api.dto;

import java.util.List;

public class GuestContentFetchRequest {

    /**
     * Optional: restrict random picks to these topicIds.
     * If null/empty → any topic.
     */
    private List<Long> topicIds;

    /**
     * Number of items to fetch (default = 5 if null/<=0).
     */
    private Integer size;

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
}