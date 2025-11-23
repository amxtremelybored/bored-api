// src/main/java/in/bored/api/model/UserProfile.java
package in.bored.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "uid", unique = true)
    private String uid;              // Firebase UID (or same as firebaseUid)

    @Column(name = "display_name")
    private String displayName;

    private Integer age;
    private String phone;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProfileStatus status = ProfileStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "firebase_uid")
    private String firebaseUid;

    @Column(name = "photo_url")
    private String photoUrl;

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<UserPreference> preferences = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ProfileStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public ProfileStatus getStatus() { return status; }
    public void setStatus(ProfileStatus status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getFirebaseUid() { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public List<UserPreference> getPreferences() { return preferences; }
    public void setPreferences(List<UserPreference> preferences) { this.preferences = preferences; }
}