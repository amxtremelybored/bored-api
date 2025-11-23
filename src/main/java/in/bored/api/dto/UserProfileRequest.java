// src/main/java/in/bored/api/dto/UserProfileRequest.java
package in.bored.api.dto;

public class UserProfileRequest {

    private String displayName;
    private Integer age;
    private String phone;
    private String email;
    private String photoUrl;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}