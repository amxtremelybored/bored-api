// src/main/java/in/bored/api/service/ContentFeedService.java
package in.bored.api.service;

import in.bored.api.dto.ContentFetchRequest;
import in.bored.api.dto.ContentItemResponse;
import in.bored.api.model.*;
import in.bored.api.repo.TopicContentRepository;
import in.bored.api.repo.TopicRepository;
import in.bored.api.repo.UserContentViewRepository;
import in.bored.api.repo.UserPreferenceRepository;
import in.bored.api.repo.UserProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContentFeedService {

    private final TopicContentRepository topicContentRepository;
    private final TopicRepository topicRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserContentViewRepository userContentViewRepository;

    public ContentFeedService(TopicContentRepository topicContentRepository,
                              TopicRepository topicRepository,
                              UserProfileRepository userProfileRepository,
                              UserPreferenceRepository userPreferenceRepository,
                              UserContentViewRepository userContentViewRepository) {
        this.topicContentRepository = topicContentRepository;
        this.topicRepository = topicRepository;
        this.userProfileRepository = userProfileRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.userContentViewRepository = userContentViewRepository;
    }

    public List<ContentItemResponse> fetchNextForCurrentUser(ContentFetchRequest request) {
        UserProfile profile = getCurrentUserProfile();

        // 1) Resolve topics
        List<Topic> topics = resolveTopics(profile, request.getTopicIds());
        if (topics.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) Size (default 5)
        int size = (request.getSize() != null && request.getSize() > 0)
                ? request.getSize()
                : 5;

        Pageable pageable = PageRequest.of(0, size);

        // 3) Find unseen content for this user across those topics
        List<TopicContent> contents =
                topicContentRepository.findNextUnseenForUser(profile, topics, pageable);

        if (contents.isEmpty()) {
            return Collections.emptyList();
        }

        // 4) Mark as viewed (so they won't be shown again)
        List<UserContentView> views = contents.stream()
                .map(c -> {
                    UserContentView v = new UserContentView();
                    v.setUserProfile(profile);
                    v.setTopicContent(c);
                    return v;
                })
                .toList();

        userContentViewRepository.saveAll(views);

        // 5) Map to response DTO
        return contents.stream()
                .map(this::toResponse)
                .toList();
    }

    private List<Topic> resolveTopics(UserProfile profile, List<Long> topicIds) {
        if (topicIds != null && !topicIds.isEmpty()) {
            // use only requested topics
            List<Topic> topics = topicRepository.findAllById(topicIds);
            if (topics.size() != topicIds.size()) {
                throw new ResourceNotFoundException("One or more topicIds are invalid");
            }
            return topics;
        }

        // else: use topics from user_preferences
        List<UserPreference> prefs = userPreferenceRepository.findByUserProfile(profile);
        return prefs.stream()
                .map(UserPreference::getTopic)
                .distinct()
                .toList();
    }

    private ContentItemResponse toResponse(TopicContent c) {
        ContentItemResponse dto = new ContentItemResponse();
        dto.setId(c.getId());
        dto.setTopicId(c.getTopic().getId());
        dto.setTopicName(c.getTopic().getName());
        dto.setTopicEmoji(c.getTopic().getEmoji());
        dto.setContentIndex(c.getContentIndex());
        dto.setContent(c.getContent());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    private UserProfile getCurrentUserProfile() {
        String uid = getCurrentUid();
        return userProfileRepository.findByUidAndStatusNot(uid, ProfileStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found or deleted for uid: " + uid));
    }

    private String getCurrentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return auth.getPrincipal().toString();
    }
}