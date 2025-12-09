// src/main/java/in/bored/api/service/ContentFeedService.java
package in.bored.api.service;

import in.bored.api.dto.ContentCategorySummary;
import in.bored.api.dto.ContentEmptyMetaResponse;
import in.bored.api.dto.ContentFetchRequest;
import in.bored.api.dto.ContentItemResponse;
import in.bored.api.dto.GuestContentFetchRequest;
import in.bored.api.dto.TopicSummary;
import in.bored.api.model.ContentCategory;
import in.bored.api.model.ProfileStatus;
import in.bored.api.model.Topic;
import in.bored.api.model.TopicContent;
import in.bored.api.model.UserContentView;
import in.bored.api.model.UserPreference;
import in.bored.api.model.UserProfile;
import in.bored.api.repo.ContentCategoryRepository;
import in.bored.api.repo.TopicContentRepository;
import in.bored.api.repo.TopicRepository;
import in.bored.api.repo.UserContentViewRepository;
import in.bored.api.repo.UserPreferenceRepository;
import in.bored.api.repo.UserProfileRepository;
import in.bored.api.repo.UserSearchLogRepository; // New Import
import in.bored.api.model.UserSearchLog; // New Import
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ContentFeedService {

    private static final Logger logger = LoggerFactory.getLogger(ContentFeedService.class);

    private final TopicContentRepository topicContentRepository;
    private final TopicRepository topicRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final UserContentViewRepository userContentViewRepository;
    private final ContentCategoryRepository contentCategoryRepository;
    private final GeminiService geminiService;
    private final UserSearchLogRepository userSearchLogRepository; // New Field

    public ContentFeedService(TopicContentRepository topicContentRepository,
            TopicRepository topicRepository,
            UserProfileRepository userProfileRepository,
            UserPreferenceRepository userPreferenceRepository,
            UserContentViewRepository userContentViewRepository,
            ContentCategoryRepository contentCategoryRepository,
            GeminiService geminiService,
            UserSearchLogRepository userSearchLogRepository) {
        this.topicContentRepository = topicContentRepository;
        this.topicRepository = topicRepository;
        this.userProfileRepository = userProfileRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.userContentViewRepository = userContentViewRepository;
        this.contentCategoryRepository = contentCategoryRepository;
        this.geminiService = geminiService;
        this.userSearchLogRepository = userSearchLogRepository;
    }

    // ---------------------------------------------------------
    // 1) LOGGED-IN feed: unseen items based on prefs / topics
    // ---------------------------------------------------------
    public List<ContentItemResponse> fetchNextForCurrentUser(ContentFetchRequest request) {
        UserProfile profile = getCurrentUserProfile();

        boolean refreshTopic = (request != null && request.getRefreshTopic() != null && request.getRefreshTopic());

        List<Topic> topics;
        if (refreshTopic) {
            // "I'm Bored" clicked -> Pick a NEW random topic from user's categories
            // 1. Get user's preferred categories
            List<UserPreference> prefs = userPreferenceRepository.findByUserProfile(profile);
            List<ContentCategory> categories = prefs.stream()
                    .map(UserPreference::getCategory)
                    .distinct()
                    .toList();

            // 2. Get all topics for these categories
            List<Topic> allTopics;
            if (categories.isEmpty()) {
                // No preferences -> Use ALL topics
                allTopics = topicRepository.findAllByContentLoadedTrue();
            } else {
                allTopics = topicRepository.findByCategoryInAndContentLoadedTrue(categories);
            }

            if (allTopics.isEmpty()) {
                // Fallback: No loaded topics? Try ALL topics for these categories
                if (categories.isEmpty()) {
                    allTopics = topicRepository.findAll();
                } else {
                    allTopics = topicRepository.findByCategoryIn(categories);
                }
            }

            if (allTopics.isEmpty()) {
                return Collections.emptyList();
            }

            // 3. Pick one random topic
            Topic randomTopic = allTopics.get(new java.util.Random().nextInt(allTopics.size()));
            topics = List.of(randomTopic);

        } else {
            // Normal flow: STRICTLY stick to requested topics.
            // "topicIds is must all logic shpuld be based on that"
            if (request.getTopicIds() == null || request.getTopicIds().isEmpty()) {
                logger.warn(
                        "⚠️ fetchNextForCurrentUser: topicIds missing in request. Returning empty list (no fallback to prefs).");
                return Collections.emptyList();
            }
            topics = topicRepository.findAllById(request.getTopicIds());
            if (topics.size() != request.getTopicIds().size()) {
                logger.warn("⚠️ fetchNextForCurrentUser: Some requested topicIds not found.");
            }
        }

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
                logger.info("⚠️ No DB content for user. Falling back to Gemini for topic: {}", fallbackTopic.getName());

                String catName = (fallbackTopic.getCategory() != null) ? fallbackTopic.getCategory().getName()
                        : "General";

                List<ContentItemResponse> generatedItems = geminiService.generateContent(
                        fallbackTopic.getName(),
                        catName,
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
        boolean refresh = (request != null && request.getRefreshContent() != null && request.getRefreshContent());

        int size = (request != null && request.getSize() != null && request.getSize() > 0)
                ? request.getSize()
                : 5;

        Pageable pageable = PageRequest.of(0, size);

        // 1. REFRESH or INITIAL LOAD (no topicIds): Pick ONE random topic
        if (refresh || (topicIds == null || topicIds.isEmpty())) {
            // First, try to find a topic that HAS content
            Topic randomTopic = topicRepository.findRandomTopicWithContent();

            // If DB is completely empty (no topics have content), pick ANY topic
            if (randomTopic == null) {
                randomTopic = topicRepository.findRandomTopic();
            }

            if (randomTopic == null) {
                return Collections.emptyList();
            }

            // Fetch content for this specific topic
            List<TopicContent> contents = topicContentRepository.findRandomByTopicIn(
                    List.of(randomTopic),
                    pageable);

            // If DB empty for this topic, try Gemini fallback
            if (contents.isEmpty()) {
                logger.info("⚠️ No DB content for guest (random). Falling back to Gemini for topic: {}",
                        randomTopic.getName());
                String catName = (randomTopic.getCategory() != null) ? randomTopic.getCategory().getName() : "General";

                List<ContentItemResponse> generatedItems = geminiService.generateContent(
                        randomTopic.getName(),
                        catName,
                        size);

                if (!generatedItems.isEmpty()) {
                    // Calculate next content index
                    Integer maxIndex = topicContentRepository.findMaxContentIndexByTopic(randomTopic);
                    int nextIndex = (maxIndex == null) ? 0 : maxIndex + 1;

                    // Persist new content
                    List<TopicContent> newContents = new java.util.ArrayList<>();
                    for (ContentItemResponse item : generatedItems) {
                        TopicContent tc = new TopicContent();
                        tc.setTopic(randomTopic);
                        tc.setContent(item.getContent());
                        tc.setContentIndex(nextIndex++);
                        tc.setCreatedAt(java.time.OffsetDateTime.now());
                        newContents.add(tc);
                    }
                    List<TopicContent> savedContents = topicContentRepository.saveAll(newContents);

                    // Update flags for Topic and Category
                    java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

                    randomTopic.setContentLoaded(true);
                    randomTopic.setContentLoadedAt(now);
                    topicRepository.save(randomTopic);

                    ContentCategory category = randomTopic.getCategory();
                    if (category != null) {
                        category.setContentLoaded(true);
                        category.setContentLoadedAt(now);
                        contentCategoryRepository.save(category);
                    }

                    // If guestUid provided, save views for generated content too!
                    if (request != null && request.getGuestUid() != null && !request.getGuestUid().isEmpty()) {
                        List<UserContentView> views = new java.util.ArrayList<>();
                        for (TopicContent tc : savedContents) {
                            UserContentView view = new UserContentView();
                            view.setGuestUid(request.getGuestUid()); // Set guest UID directly
                            view.setUserProfile(null); // No profile for guests
                            view.setTopicContent(tc);
                            view.setTopic(tc.getTopic());
                            views.add(view);
                        }
                        userContentViewRepository.saveAll(views);
                    }

                    return savedContents.stream()
                            .map(tc -> {
                                ContentItemResponse resp = toResponse(tc);
                                resp.setSource("Gemini");
                                return resp;
                            })
                            .toList();
                }
                return Collections.emptyList();
            }

            // If we have content from DB
            // If guestUid provided, save views!
            if (request != null && request.getGuestUid() != null && !request.getGuestUid().isEmpty()) {
                List<UserContentView> views = new java.util.ArrayList<>();
                for (TopicContent tc : contents) {
                    UserContentView view = new UserContentView();
                    view.setGuestUid(request.getGuestUid()); // Set guest UID directly
                    view.setUserProfile(null); // No profile for guests
                    view.setTopicContent(tc);
                    view.setTopic(tc.getTopic());
                    views.add(view);
                }
                userContentViewRepository.saveAll(views);
            }

            return contents.stream()
                    .map(this::toResponse)
                    .toList();
        }

        // 2. STICKY LOAD (has topicIds and NOT refreshing): Stick to provided topics
        List<Topic> topics = topicRepository.findAllById(topicIds);
        if (topics.isEmpty()) {
            return Collections.emptyList();
        }

        List<TopicContent> contents;
        if (request != null && request.getGuestUid() != null && !request.getGuestUid().isEmpty()) {
            contents = topicContentRepository.findRandomUnseenForGuest(topics, request.getGuestUid(), pageable);
        } else {
            contents = topicContentRepository.findRandomByTopicIn(topics, pageable);
        }

        // Fallback: If no content in DB, try Gemini (only if single topic requested)
        if ((contents == null || contents.isEmpty()) && topics.size() == 1) {
            Topic targetTopic = topics.get(0);
            logger.info("⚠️ No DB content for guest (sticky). Falling back to Gemini for topic: {}",
                    targetTopic.getName());
            String catName = (targetTopic.getCategory() != null) ? targetTopic.getCategory().getName() : "General";

            List<ContentItemResponse> generatedItems = geminiService.generateContent(
                    targetTopic.getName(),
                    catName,
                    size);

            if (!generatedItems.isEmpty()) {
                Integer maxIndex = topicContentRepository.findMaxContentIndexByTopic(targetTopic);
                int nextIndex = (maxIndex == null) ? 0 : maxIndex + 1;

                List<TopicContent> newContents = new java.util.ArrayList<>();
                for (ContentItemResponse item : generatedItems) {
                    TopicContent tc = new TopicContent();
                    tc.setTopic(targetTopic);
                    tc.setContent(item.getContent());
                    tc.setContentIndex(nextIndex++);
                    tc.setCreatedAt(java.time.OffsetDateTime.now());
                    newContents.add(tc);
                }
                List<TopicContent> savedContents = topicContentRepository.saveAll(newContents);

                // Update flags
                java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
                targetTopic.setContentLoaded(true);
                targetTopic.setContentLoadedAt(now);
                topicRepository.save(targetTopic);

                ContentCategory category = targetTopic.getCategory();
                if (category != null) {
                    category.setContentLoaded(true);
                    category.setContentLoadedAt(now);
                    contentCategoryRepository.save(category);
                }

                // Save history if guestUid present
                if (request != null && request.getGuestUid() != null && !request.getGuestUid().isEmpty()) {
                    List<UserContentView> views = new java.util.ArrayList<>();
                    for (TopicContent tc : savedContents) {
                        UserContentView view = new UserContentView();
                        view.setGuestUid(request.getGuestUid());
                        view.setUserProfile(null);
                        view.setTopicContent(tc);
                        view.setTopic(tc.getTopic());
                        views.add(view);
                    }
                    userContentViewRepository.saveAll(views);
                }

                return savedContents.stream()
                        .map(tc -> {
                            ContentItemResponse resp = toResponse(tc);
                            resp.setSource("Gemini");
                            return resp;
                        })
                        .toList();
            }
        }

        if (contents == null || contents.isEmpty()) {
            return Collections.emptyList();
        }

        // Save history if guestUid present
        if (request != null && request.getGuestUid() != null && !request.getGuestUid().isEmpty()) {
            List<UserContentView> views = new java.util.ArrayList<>();
            for (TopicContent tc : contents) {
                UserContentView view = new UserContentView();
                view.setGuestUid(request.getGuestUid());
                view.setUserProfile(null);
                view.setTopicContent(tc);
                view.setTopic(tc.getTopic());
                views.add(view);
            }
            userContentViewRepository.saveAll(views);
        }

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
                : topicRepository.findByCategoryInAndContentLoadedTrue(prefCategories);

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

        return topicRepository.findByCategoryInAndContentLoadedTrue(categories);
    }

    private ContentItemResponse toResponse(TopicContent c) {
        ContentItemResponse dto = new ContentItemResponse();
        dto.setId(c.getId());
        dto.setTopicId(c.getTopic().getId());
        dto.setTopicName(c.getTopic().getName());
        dto.setTopicDisplayName(c.getTopic().getDisplayName());
        dto.setTopicEmoji(c.getTopic().getEmoji());
        dto.setContentIndex(c.getContentIndex());
        dto.setContent(c.getContent());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setSource("API");
        dto.setCategoryName(c.getTopic().getCategory().getName());
        return dto;
    }

    public TopicSummary getNextTopicForUser(Long currentTopicId) {
        UserProfile profile = getCurrentUserProfile();

        // 1. Get user's preferred categories
        List<UserPreference> prefs = userPreferenceRepository.findByUserProfile(profile);
        List<ContentCategory> categories = prefs.stream()
                .map(UserPreference::getCategory)
                .distinct()
                .toList();

        // 2. Get all topics for these categories
        List<Topic> allTopics;
        if (categories.isEmpty()) {
            // No preferences (e.g. new guest) -> Use ALL topics
            allTopics = topicRepository.findAllByContentLoadedTrue();
        } else {
            allTopics = topicRepository.findByCategoryInAndContentLoadedTrue(categories);
        }

        // 3. Filter out current topic if possible
        List<Topic> candidates = new java.util.ArrayList<>(allTopics);
        if (currentTopicId != null) {
            candidates.removeIf(t -> t.getId().equals(currentTopicId));
        }

        // 4. Filter out recently viewed topics (avoid repetition)
        // Fetch last 10000 viewed topics (effectively "all" for now)
        Pageable pageable = PageRequest.of(0, 10000);
        List<Long> recentTopicIds = userContentViewRepository.findRecentTopicIds(profile, pageable);

        if (!recentTopicIds.isEmpty()) {
            candidates.removeIf(t -> recentTopicIds.contains(t.getId()));
        }

        // 5. If candidates empty, try falling back to ALL topics (including empty ones)
        if (candidates.isEmpty()) {
            logger.info("⚠️ No pre-loaded topics left for user. Falling back to ALL topics.");
            List<Topic> fallbackTopics;
            if (categories.isEmpty()) {
                fallbackTopics = topicRepository.findAll();
            } else {
                fallbackTopics = topicRepository.findByCategoryIn(categories);
            }

            // Filter viewed again
            candidates = new java.util.ArrayList<>(fallbackTopics);
            if (currentTopicId != null) {
                candidates.removeIf(t -> t.getId().equals(currentTopicId));
            }
            if (!recentTopicIds.isEmpty()) {
                candidates.removeIf(t -> recentTopicIds.contains(t.getId()));
            }
        }

        // 6. Pick one random topic
        if (candidates.isEmpty()) {
            // "return []" -> No new topics available.
            throw new TopicsExhaustedException("No new topics available");
        }
        Topic randomTopic = candidates.get(new java.util.Random().nextInt(candidates.size()));

        // 6. Convert to TopicSummary
        TopicSummary summary = new TopicSummary();
        summary.setId(randomTopic.getId());
        summary.setName(randomTopic.getName());
        summary.setEmoji(randomTopic.getEmoji());
        summary.setCategoryId(randomTopic.getCategory().getId());

        return summary;
    }

    public TopicSummary getNextTopicForGuest(String guestUid, List<Long> seenTopicIds) {
        // 1. Fetch all topics (or a large random subset)
        List<Topic> allTopics = topicRepository.findAllByContentLoadedTrue();

        if (allTopics.isEmpty()) {
            throw new ResourceNotFoundException("No topics found");
        }

        // 2. Filter out seen topics (from payload AND from DB if guestUid exists)
        List<Topic> candidates = new java.util.ArrayList<>(allTopics);

        // Filter from payload
        if (seenTopicIds != null && !seenTopicIds.isEmpty()) {
            candidates.removeIf(t -> seenTopicIds.contains(t.getId()));
        }

        // Filter from DB if guestUid provided
        List<Long> dbSeenIds = new java.util.ArrayList<>();
        if (guestUid != null && !guestUid.isEmpty()) {
            Pageable pageable = PageRequest.of(0, 10000);
            dbSeenIds = userContentViewRepository.findRecentTopicIdsForGuest(guestUid, pageable);
            if (!dbSeenIds.isEmpty()) {
                final List<Long> finalDbSeenIds = dbSeenIds;
                candidates.removeIf(t -> finalDbSeenIds.contains(t.getId()));
            }
        }

        // 3. If candidates empty, fallback to ALL topics
        if (candidates.isEmpty()) {
            logger.info("⚠️ No pre-loaded topics left for guest. Falling back to ALL topics.");
            List<Topic> fallbackTopics = topicRepository.findAll();
            candidates = new java.util.ArrayList<>(fallbackTopics);

            // Re-filter
            if (seenTopicIds != null && !seenTopicIds.isEmpty()) {
                candidates.removeIf(t -> seenTopicIds.contains(t.getId()));
            }
            if (!dbSeenIds.isEmpty()) {
                final List<Long> finalDbSeenIds = dbSeenIds;
                candidates.removeIf(t -> finalDbSeenIds.contains(t.getId()));
            }
        }

        // 4. Pick one random topic
        if (candidates.isEmpty()) {
            // Fallback: if user has seen everything, return empty/error
            throw new TopicsExhaustedException("No new topics available");
        }

        Topic randomTopic = candidates.get(new java.util.Random().nextInt(candidates.size()));

        // 4. Convert to TopicSummary
        TopicSummary summary = new TopicSummary();
        summary.setId(randomTopic.getId());
        summary.setName(randomTopic.getName());
        summary.setEmoji(randomTopic.getEmoji());
        summary.setCategoryId(randomTopic.getCategory().getId());

        return summary;
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
                .orElseGet(() -> {
                    // Auto-create profile for new users (including guests)
                    logger.info("Creating new UserProfile for uid: {}", uid);
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUid(uid);
                    newProfile.setFirebaseUid(uid); // Mandatory field
                    newProfile.setStatus(ProfileStatus.ACTIVE);
                    return userProfileRepository.save(newProfile);
                });
    }

    private String getCurrentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return auth.getPrincipal().toString();
    }

    // ---------------------------------------------------------
    // 4) SEARCH (Unified: DB -> Gemini)
    // ---------------------------------------------------------
    // Unified Search with Logging
    public List<ContentItemResponse> searchContent(String query, int size, String guestUid) {
        String trimmedQuery = (query == null || query.trim().isEmpty()) ? "" : query.trim();
        if (trimmedQuery.isEmpty()) {
            return Collections.emptyList();
        }

        // Resolve UserProfile if authenticated logic remains same...
        UserProfile user = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                user = getCurrentUserProfile();
            }
        } catch (Exception e) {
            // Ignore
        }

        // 📝 Log the search
        try {
            UserSearchLog log = new UserSearchLog();
            log.setSearchQuery(trimmedQuery);
            log.setUserProfile(user);
            log.setGuestUid(guestUid);
            userSearchLogRepository.save(log);
        } catch (Exception e) {
            logger.error("Failed to log search query", e);
        }

        // ⚡️ DIRECT AI SEARCH (No DB Topic Lookup)
        try {
            // 1. Get raw JSON from Gemini
            String generatedJson = geminiService.generateContent(trimmedQuery, size);

            // 2. Parse JSON to Objects
            List<ContentItemResponse> generatedItems = geminiService.parseContent(generatedJson, trimmedQuery);

            // Assign dummy IDs and Index to avoid Flutter Null check errors
            long dummyIdCounter = System.currentTimeMillis();
            int indexCounter = 0;

            for (ContentItemResponse item : generatedItems) {
                // Ensure ID is not null (Flutter expects int)
                if (item.getId() == null) {
                    item.setId(dummyIdCounter++);
                }

                // Ensure ContentIndex is not null
                if (item.getContentIndex() == null) {
                    item.setContentIndex(indexCounter++);
                }

                // Ensure TopicID is not null (use 0 or -1 for transient)
                if (item.getTopicId() == null) {
                    item.setTopicId(0L);
                }

                item.setSource("Gemini (Direct)");
                item.setTopicName(trimmedQuery);
                item.setTopicDisplayName("Search: " + trimmedQuery);

                // Fix for Flutter Null check on createdAt
                if (item.getCreatedAt() == null) {
                    item.setCreatedAt(java.time.OffsetDateTime.now());
                }
            }

            return generatedItems;

        } catch (Exception e) {
            logger.error("Direct search failed", e);
            return Collections.emptyList();
        }
    }

    private ContentItemResponse mapToResponse(TopicContent tc) {
        ContentItemResponse resp = new ContentItemResponse();
        resp.setId(tc.getId());
        resp.setContent(tc.getContent());
        resp.setTopicName(tc.getTopic().getName());
        resp.setTopicDisplayName(tc.getTopic().getDisplayName());
        if (tc.getTopic().getCategory() != null) {
            resp.setCategoryName(tc.getTopic().getCategory().getName());
        }
        resp.setSource(tc.getSource());
        resp.setCreatedAt(tc.getCreatedAt());
        // Note: Saved status is context-dependent.
        // For unified search/feed, we currently don't pre-populate 'saved' to avoid
        // N+1.
        // It defaults to false.
        return resp;
    }

    // ---------------------------------------------------------
    // 5) TOGGLE SAVE
    // ---------------------------------------------------------
    public ContentItemResponse toggleSave(Long contentId, boolean saved, String guestUid) {
        // 1. Resolve User
        UserProfile user = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                user = getCurrentUserProfile();
            }
        } catch (Exception e) {
            // Ignore
        }

        // 2. Fetch Content
        TopicContent content = topicContentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found: " + contentId));

        UserContentView view;

        if (user != null) {
            // Authenticated
            final UserProfile finalUser = user;
            view = userContentViewRepository.findByUserProfileAndTopicContent_Id(finalUser, contentId)
                    .orElseGet(() -> {
                        UserContentView v = new UserContentView();
                        v.setUserProfile(finalUser);
                        v.setTopicContent(content);
                        v.setTopic(content.getTopic());
                        return v;
                    });
        } else if (guestUid != null && !guestUid.isEmpty()) {
            // Guest
            view = userContentViewRepository.findByGuestUidAndTopicContent_Id(guestUid, contentId)
                    .orElseGet(() -> {
                        UserContentView v = new UserContentView();
                        v.setGuestUid(guestUid);
                        v.setTopicContent(content);
                        v.setTopic(content.getTopic());
                        return v;
                    });
        } else {
            throw new ResourceNotFoundException("User identification required");
        }

        view.setSaved(saved);
        if (view.getViewedAt() == null) {
            view.setViewedAt(java.time.OffsetDateTime.now());
        }

        UserContentView savedView = userContentViewRepository.save(view);

        ContentItemResponse response = toResponse(content);
        response.setSaved(savedView.isSaved());
        return response;
    }

}