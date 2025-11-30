// src/main/java/in/bored/api/controller/ContentFeedController.java
package in.bored.api.controller;

import in.bored.api.dto.ContentEmptyMetaResponse;
import in.bored.api.dto.ContentFetchRequest;
import in.bored.api.dto.ContentItemResponse;
import in.bored.api.dto.GuestContentFetchRequest;
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
            @RequestBody ContentFetchRequest request
    ) {
        List<ContentItemResponse> items =
                contentFeedService.fetchNextForCurrentUser(request);
        return ResponseEntity.ok(items);
    }

    // 🧠 When /next returns [], client can call this to get prefs + topics
    @PostMapping("/next/meta")
    public ResponseEntity<ContentEmptyMetaResponse> getNextContentMeta(
            @RequestBody(required = false) ContentFetchRequest request
    ) {
        ContentEmptyMetaResponse meta =
                contentFeedService.buildEmptyMetaForCurrentUser(request);
        return ResponseEntity.ok(meta);
    }

    // 🆓 GUEST users: random content, no DB user prefs / views
    @PostMapping("/guest-next")
    public ResponseEntity<List<ContentItemResponse>> getGuestContent(
            @RequestBody(required = false) GuestContentFetchRequest request
    ) {
        // request can be null – handle gracefully in service
        List<ContentItemResponse> items =
                contentFeedService.fetchRandomForGuest(request);
        return ResponseEntity.ok(items);
    }
}