// src/main/java/in/bored/api/service/ContentFeedService.java
package in.bored.api.service;

import in.bored.api.dto.ContentFetchRequest;
import in.bored.api.dto.ContentItemResponse;
import in.bored.api.dto.GuestContentFetchRequest;
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

    // ---------------------------------------------------------
    // 1) EXISTING: logged-in user feed (unseen, user prefs, views)
    // ---------------------------------------------------------
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

        // 3) Unseen content
        List<TopicContent> contents =
                topicContentRepository.findNextUnseenForUser(profile, topics, pageable);

        if (contents.isEmpty()) {
            return Collections.emptyList();
        }

        // 4) Save views (store topic too)
        List<UserContentView> views = contents.stream()
                .map(c -> {
                    UserContentView v = new UserContentView();
                    v.setUserProfile(profile);
                    v.setTopicContent(c);
                    v.setTopic(c.getTopic());
                    return v;
                })
                .toList();

        userContentViewRepository.saveAll(views);

        // 5) Map DTOs
        return contents.stream()
                .map(this::toResponse)
                .toList();
    }

    // ---------------------------------------------------------
    // 2) NEW: guest feed → random content, NO user, NO prefs, NO views
    // ---------------------------------------------------------
    public List<ContentItemResponse> fetchRandomForGuest(GuestContentFetchRequest request) {
        // Defensive defaults if request is null
        List<Long> topicIds = (request != null) ? request.getTopicIds() : null;
        int size = (request != null && request.getSize() != null && request.getSize() > 0)
                ? request.getSize()
                : 5;

        Pageable pageable = PageRequest.of(0, size);

        List<TopicContent> contents;

        // Case 1: restrict to specific topics (if provided)
        if (topicIds != null && !topicIds.isEmpty()) {
            List<Topic> topics = topicRepository.findAllById(topicIds);
            if (topics.isEmpty()) {
                return Collections.emptyList();
            }
            contents = topicContentRepository.findRandomByTopicIn(topics, pageable);
        } else {
            // Case 2: fully random across all topics
            contents = topicContentRepository.findRandom(pageable);
        }

        if (contents == null || contents.isEmpty()) {
            return Collections.emptyList();
        }

        // IMPORTANT: we DO NOT store UserContentView here → totally stateless
        return contents.stream()
                .map(this::toResponse)
                .toList();
    }

    // ----------------- existing helpers below -----------------

    private List<Topic> resolveTopics(UserProfile profile, List<Long> topicIds) {
        // Case 1: explicit topicIds
        if (topicIds != null && !topicIds.isEmpty()) {
            List<Topic> topics = topicRepository.findAllById(topicIds);
            if (topics.size() != topicIds.size()) {
                throw new ResourceNotFoundException("One or more topicIds are invalid");
            }
            return topics;
        }

        // Case 2: category-based from user preferences
        List<UserPreference> prefs = userPreferenceRepository.findByUserProfile(profile);
        if (prefs.isEmpty()) {
            return Collections.emptyList();
        }

        List<ContentCategory> categories = prefs.stream()
                .map(UserPreference::getCategory)
                .distinct()
                .toList();

        if (categories.isEmpty()) {
            return Collections.emptyList();
        }

        return topicRepository.findByCategoryIn(categories);
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
                .orElseThrow(() ->
                        new ResourceNotFoundException("UserProfile not found or deleted for uid: " + uid));
    }

    private String getCurrentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return auth.getPrincipal().toString();
    }
}