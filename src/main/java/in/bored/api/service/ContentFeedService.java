// src/main/java/in/bored/api/service/ContentFeedService.java
package in.bored.api.service;

import in.bored.api.dto.ContentCategorySummary;
import in.bored.api.dto.ContentEmptyMetaResponse;
import in.bored.api.dto.ContentFetchRequest;
import in.bored.api.dto.ContentItemResponse;
import in.bored.api.dto.GuestContentFetchRequest;
import in.bored.api.dto.TopicSummary;
import in.bored.api.model.ContentCategory;
import in.bored.api.model.ContentCategory;
import in.bored.api.model.ContentCategory;
import in.bored.api.model.ProfileStatus;
import in.bored.api.model.Topic;
import in.bored.api.model.TopicContent;
import in.bored.api.model.UserContentView;
import in.bored.api.model.UserPreference;
import in.bored.api.model.UserProfile;
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
    private final GeminiService geminiService;

    public ContentFeedService(TopicContentRepository topicContentRepository,
            TopicRepository topicRepository,
            UserProfileRepository userProfileRepository,
            UserPreferenceRepository userPreferenceRepository,
            UserContentViewRepository userContentViewRepository,
            GeminiService geminiService) {
        this.topicContentRepository = topicContentRepository;
        this.topicRepository = topicRepository;
        this.userProfileRepository = userProfileRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.userContentViewRepository = userContentViewRepository;
        this.geminiService = geminiService;
    }

    // ---------------------------------------------------------
    // 1) LOGGED-IN feed: unseen items based on prefs / topics
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
        List<TopicContent> contents = topicContentRepository.findNextUnseenForUser(profile, topics, pageable);

        if (contents.isEmpty()) {
            // Fallback to Gemini if we have topics but no DB content
            if (!topics.isEmpty()) {
                // Pick a random topic from the resolved list
                Topic fallbackTopic = topics.get(new java.util.Random().nextInt(topics.size()));

                List<ContentItemResponse> generatedItems = geminiService.generateContent(
                        fallbackTopic.getName(),
                        fallbackTopic.getCategory().getName(),
                        size);

                if (generatedItems.isEmpty()) {
                    return Collections.emptyList();
                }

                // Calculate next content index
                Integer maxIndex = topicContentRepository.findMaxContentIndexByTopic(fallbackTopic);
                int nextIndex = (maxIndex == null) ? 0 : maxIndex + 1;

                // Persist new content
                List<TopicContent> newContents = new java.util.ArrayList<>();
                for (ContentItemResponse item : generatedItems) {
                    TopicContent tc = new TopicContent();
                    tc.setTopic(fallbackTopic);
                    tc.setContent(item.getContent());
                    tc.setContentIndex(nextIndex++);
                    // prePersist will set createdAt
                    newContents.add(tc);
                }

                List<TopicContent> savedContents = topicContentRepository.saveAll(newContents);

                // Mark as viewed immediately
                List<UserContentView> newViews = savedContents.stream()
                        .map(c -> {
                            UserContentView v = new UserContentView();
                            v.setUserProfile(profile);
                            v.setTopicContent(c);
                            v.setTopic(c.getTopic());
                            return v;
                        })
                        .toList();

                userContentViewRepository.saveAll(newViews);

                // Return mapped response
                return savedContents.stream()
                        .map(c -> {
                            ContentItemResponse dto = this.toResponse(c);
                            dto.setSource("Gemini");
                            return dto;
                        })
                        .toList();
            }
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
    // 2) GUEST feed: random content, no prefs, no views
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

    // ---------------------------------------------------------
    // 3) "Empty feed" meta for logged-in users (prefs + topics)
    // ---------------------------------------------------------
    public ContentEmptyMetaResponse buildEmptyMetaForCurrentUser(ContentFetchRequest request) {
        UserProfile profile = getCurrentUserProfile();

        List<Long> requestedTopicIds = (request != null) ? request.getTopicIds() : null;

        // Try to reuse resolveTopics logic if topicIds are provided
        List<Topic> requestedTopics = (requestedTopicIds != null && !requestedTopicIds.isEmpty())
                ? resolveTopics(profile, requestedTopicIds)
                : Collections.emptyList();

        // Derive categories:
        // 1) from user preferences
        List<UserPreference> prefs = userPreferenceRepository.findByUserProfile(profile);
        List<ContentCategory> prefCategories = prefs.stream()
                .map(UserPreference::getCategory)
                .distinct()
                .toList();

        // 2) if prefs are empty, fall back to categories from requested topics
        if (prefCategories.isEmpty() && !requestedTopics.isEmpty()) {
            prefCategories = requestedTopics.stream()
                    .map(Topic::getCategory)
                    .distinct()
                    .toList();
        }

        // Load topics for those categories for suggestion
        List<Topic> topicsForDisplay = prefCategories.isEmpty()
                ? Collections.emptyList()
                : topicRepository.findByCategoryIn(prefCategories);

        // Map to summaries
        List<ContentCategorySummary> categorySummaries = prefCategories.stream()
                .map(this::toCategorySummary)
                .toList();

        List<TopicSummary> topicSummaries = topicsForDisplay.stream()
                .map(this::toTopicSummary)
                .toList();

        ContentEmptyMetaResponse response = new ContentEmptyMetaResponse();
        response.setEmpty(true);
        response.setPreferredCategories(categorySummaries);
        response.setSuggestedTopics(topicSummaries);

        return response;
    }

    // ----------------- existing helpers -----------------

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
        dto.setSource("API");
        dto.setCategoryName(c.getTopic().getCategory().getName());
        return dto;
    }

    private ContentCategorySummary toCategorySummary(ContentCategory category) {
        ContentCategorySummary dto = new ContentCategorySummary();
        dto.setId(category.getId()); // UUID -> UUID
        dto.setName(category.getName());
        dto.setEmoji(category.getEmoji());
        return dto;
    }

    private TopicSummary toTopicSummary(Topic topic) {
        TopicSummary dto = new TopicSummary();
        dto.setId(topic.getId()); // Long -> Long
        dto.setName(topic.getName());
        dto.setEmoji(topic.getEmoji());
        dto.setCategoryId(topic.getCategory().getId()); // UUID -> UUID
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