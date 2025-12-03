// src/main/java/in/bored/api/controller/ContentFeedController.java
package in.bored.api.controller;

import in.bored.api.dto.ContentEmptyMetaResponse;
import in.bored.api.dto.ContentFetchRequest;
import in.bored.api.dto.ContentItemResponse;
import in.bored.api.dto.GuestContentFetchRequest;
import in.bored.api.dto.TopicSummary;
import in.bored.api.service.ContentFeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentFeedController {

        private final ContentFeedService contentFeedService;

        public ContentFeedController(ContentFeedService contentFeedService) {
                this.contentFeedService = contentFeedService;
        }

        // 🔐 Authenticated users: unseen feed based on prefs
        @PostMapping("/next")
        public ResponseEntity<List<ContentItemResponse>> getNextContent(
                        @RequestBody(required = false) ContentFetchRequest request) {

                // Check if user is authenticated
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();

                boolean isGuest = auth == null || !auth.isAuthenticated()
                                || "anonymousUser".equals(auth.getPrincipal());

                if (isGuest) {
                        // Delegate to guest logic
                        GuestContentFetchRequest guestRequest = new GuestContentFetchRequest();
                        if (request != null) {
                                guestRequest.setTopicIds(request.getTopicIds());
                                guestRequest.setSize(request.getSize());
                                guestRequest.setRefreshContent(request.getRefreshTopic());
                        }

                        // Extract guest UID from header if present (Firebase anon auth)
                        if (auth != null && "anonymousUser".equals(auth.getPrincipal()) == false) {
                                // If principal is a String UID (from Firebase filter)
                                if (auth.getPrincipal() instanceof String) {
                                        guestRequest.setGuestUid((String) auth.getPrincipal());
                                }
                        }

                        return ResponseEntity.ok(contentFeedService.fetchRandomForGuest(guestRequest));
                }

                // Authenticated user logic
                List<ContentItemResponse> items = contentFeedService.fetchNextForCurrentUser(request);
                return ResponseEntity.ok(items);
        }

        @PostMapping("/topic-next")
        public ResponseEntity<TopicSummary> getNextTopic(
                        @RequestBody(required = false) java.util.Map<String, Object> payload) {
                Long currentTopicId = null;
                if (payload != null && payload.containsKey("currentTopicId")) {
                        Object val = payload.get("currentTopicId");
                        if (val instanceof Number) {
                                currentTopicId = ((Number) val).longValue();
                        }
                }
                TopicSummary summary = contentFeedService.getNextTopicForUser(currentTopicId);
                return ResponseEntity.ok(summary);
        }

        // 🧠 When /next returns [], client can call this to get prefs + topics
        @PostMapping("/next/meta")
        public ResponseEntity<ContentEmptyMetaResponse> getNextContentMeta(
                        @RequestBody(required = false) ContentFetchRequest request) {
                ContentEmptyMetaResponse meta = contentFeedService.buildEmptyMetaForCurrentUser(request);
                return ResponseEntity.ok(meta);
        }

        // 🆓 GUEST users: random content, no DB user prefs / views
        @PostMapping("/guest-next")
        public ResponseEntity<List<ContentItemResponse>> getGuestContent(
                        @RequestBody(required = false) GuestContentFetchRequest request) {

                // Extract UID from header if present
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();
                String headerUid = null;
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                        headerUid = (String) auth.getPrincipal();
                }

                if (request == null) {
                        request = new GuestContentFetchRequest();
                }

                // Header takes precedence, or fallback to body if header missing (though body
                // field might be deprecated)
                if (headerUid != null) {
                        request.setGuestUid(headerUid);
                }

                List<ContentItemResponse> items = contentFeedService.fetchRandomForGuest(request);
                return ResponseEntity.ok(items);
        }

        @PostMapping("/guest-topic-next")
        public ResponseEntity<TopicSummary> getGuestNextTopic(
                        @RequestBody(required = false) java.util.Map<String, Object> payload) {
                // Extract UID from header if present
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication();
                String headerUid = null;
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                        headerUid = (String) auth.getPrincipal();
                }

                String payloadUid = (payload != null) ? (String) payload.get("guestUid") : null;
                String guestUid = (headerUid != null) ? headerUid : payloadUid;

                // Backward compatibility: still accept seenTopicIds if provided (optional)
                List<Long> seenTopicIds = null;
                if (payload != null && payload.containsKey("seenTopicIds")) {
                        try {
                                seenTopicIds = (List<Long>) payload.get("seenTopicIds");
                        } catch (Exception e) {
                                // ignore if format is wrong
                        }
                }

                TopicSummary summary = contentFeedService.getNextTopicForGuest(guestUid, seenTopicIds);
                return ResponseEntity.ok(summary);
        }
}