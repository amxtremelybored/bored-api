// src/main/java/in/bored/api/dto/UserPreferenceRequest.java
package in.bored.api.dto;

import java.util.UUID;

public class UserPreferenceRequest {

    private UUID categoryId;

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
}