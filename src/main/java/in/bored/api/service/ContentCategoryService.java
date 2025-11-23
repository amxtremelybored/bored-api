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

    public ContentCategoryService(ContentCategoryRepository repository) {
        this.repository = repository;
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
                        "ContentCategory not found for id: " + id
                ));
        return toResponse(entity);
    }

    public ContentCategoryResponse create(ContentCategoryRequest request) {
        if (repository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException(
                    "ContentCategory with name '" + request.getName() + "' already exists"
            );
        }

        ContentCategory entity = new ContentCategory();
        applyRequestToEntity(request, entity, true);

        ContentCategory saved = repository.save(entity);
        return toResponse(saved);
    }

    public ContentCategoryResponse update(UUID id, ContentCategoryRequest request) {
        ContentCategory entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "ContentCategory not found for id: " + id
                ));

        repository.findByNameIgnoreCase(request.getName())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new IllegalArgumentException(
                            "Another ContentCategory with name '" + request.getName() + "' already exists"
                    );
                });

        applyRequestToEntity(request, entity, false);

        ContentCategory saved = repository.save(entity);
        return toResponse(saved);
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            return;
        }
        repository.deleteById(id);
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
            entity.setContentLoadedAt(OffsetDateTime.now());
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
                entity.getUpdatedAt()
        );
    }
}