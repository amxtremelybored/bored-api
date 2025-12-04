// src/main/java/in/bored/api/service/ContentCategoryService.java
package in.bored.api.service;

import in.bored.api.dto.ContentCategoryRequest;
import in.bored.api.dto.ContentCategoryResponse;
import in.bored.api.model.ContentCategory;
import in.bored.api.repo.ContentCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ContentCategoryService {

    private final ContentCategoryRepository repository;
    private final in.bored.api.repo.TopicRepository topicRepository;
    private final in.bored.api.repo.UserPreferenceRepository userPreferenceRepository;
    private final in.bored.api.repo.TopicContentRepository topicContentRepository;
    private final in.bored.api.repo.UserContentViewRepository userContentViewRepository;
    private final in.bored.api.repo.UserProfileRepository userProfileRepository;

    public ContentCategoryService(ContentCategoryRepository repository,
            in.bored.api.repo.TopicRepository topicRepository,
            in.bored.api.repo.UserPreferenceRepository userPreferenceRepository,
            in.bored.api.repo.TopicContentRepository topicContentRepository,
            in.bored.api.repo.UserContentViewRepository userContentViewRepository,
            in.bored.api.repo.UserProfileRepository userProfileRepository) {
        this.repository = repository;
        this.topicRepository = topicRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.topicContentRepository = topicContentRepository;
        this.userContentViewRepository = userContentViewRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<ContentCategoryResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContentCategoryResponse getById(UUID id) {
        ContentCategory entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "ContentCategory not found for id: " + id));
        return toResponse(entity);
    }

    public ContentCategoryResponse create(ContentCategoryRequest request) {
        if (repository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException(
                    "ContentCategory with name '" + request.getName() + "' already exists");
        }

        ContentCategory entity = new ContentCategory();
        applyRequestToEntity(request, entity, true);

        ContentCategory saved = repository.save(entity);
        return toResponse(saved);
    }

    public ContentCategoryResponse update(UUID id, ContentCategoryRequest request) {
        ContentCategory entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "ContentCategory not found for id: " + id));

        repository.findByNameIgnoreCase(request.getName())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException(
                            "Another ContentCategory with name '" + request.getName() + "' already exists");
                });

        applyRequestToEntity(request, entity, false);

        ContentCategory saved = repository.save(entity);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        ContentCategory category = repository.findById(id).orElse(null);
        if (category == null) {
            return;
        }

        // 1. Delete user preferences linked to this category
        userPreferenceRepository.deleteByCategory(category);

        // 2. Delete topics linked to this category
        List<in.bored.api.model.Topic> topics = topicRepository.findByCategoryIn(List.of(category));
        for (in.bored.api.model.Topic topic : topics) {
            // 2a. Delete topic contents
            topicContentRepository.deleteByTopic(topic);
            // 2b. Delete user content views
            userContentViewRepository.deleteByTopic(topic);
            // 2c. Delete topic
            topicRepository.delete(topic);
        }

        // 3. Delete category
        repository.delete(category);
    }

    public void removePreference(UUID categoryId) {
        ContentCategory category = repository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("ContentCategory not found: " + categoryId));

        in.bored.api.model.UserProfile profile = getCurrentUserProfile();

        userPreferenceRepository.findByUserProfileAndCategory(profile, category)
                .ifPresent(userPreferenceRepository::delete);
    }

    private void applyRequestToEntity(ContentCategoryRequest request,
            ContentCategory entity,
            boolean isCreate) {

        entity.setName(request.getName().trim());
        entity.setEmoji(request.getEmoji());

        Boolean contentLoadedReq = request.getContentLoaded();
        boolean contentLoaded = contentLoadedReq != null && contentLoadedReq;
        entity.setContentLoaded(contentLoaded);

        if (request.getContentLoadedAt() != null) {
            entity.setContentLoadedAt(request.getContentLoadedAt());
        } else if (isCreate && contentLoaded && entity.getContentLoadedAt() == null) {
            entity.setContentLoadedAt(java.time.OffsetDateTime.now());
        }
    }

    private ContentCategoryResponse toResponse(ContentCategory entity) {
        return new ContentCategoryResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmoji(),
                entity.isContentLoaded(),
                entity.getContentLoadedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private in.bored.api.model.UserProfile getCurrentUserProfile() {
        String uid = getCurrentUid();
        return userProfileRepository.findByUidAndStatusNot(uid, in.bored.api.model.ProfileStatus.DELETED)
                .orElseThrow(() -> new IllegalArgumentException("UserProfile not found for uid: " + uid));
    }

    private String getCurrentUid() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return auth.getPrincipal().toString();
    }
}