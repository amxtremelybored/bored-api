// src/main/java/in/bored/api/dto/UserPreferenceBulkRequest.java
package in.bored.api.dto;

import java.util.List;
import java.util.UUID;

public class UserPreferenceBulkRequest {

    private List<UUID> categoryIds;

    public List<UUID> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<UUID> categoryIds) { this.categoryIds = categoryIds; }
}