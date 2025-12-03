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

    /**
     * If true, ignore topicIds and pick a new random topic.
     */
    private Boolean refreshContent;

    /**
     * Optional: Firebase UID for guest users to track history.
     */
    private String guestUid;

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

    public Boolean getRefreshContent() {
        return refreshContent;
    }

    public void setRefreshContent(Boolean refreshContent) {
        this.refreshContent = refreshContent;
    }

    public String getGuestUid() {
        return guestUid;
    }

    public void setGuestUid(String guestUid) {
        this.guestUid = guestUid;
    }
}