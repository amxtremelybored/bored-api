// src/main/java/in/bored/api/dto/UserPreferenceBulkRequest.java
package in.bored.api.dto;

import java.util.List;

public class UserPreferenceBulkRequest {

    private List<Long> topicIds;

    public List<Long> getTopicIds() { return topicIds; }
    public void setTopicIds(List<Long> topicIds) { this.topicIds = topicIds; }
}