// src/main/java/in/bored/api/controller/ContentFeedController.java
package in.bored.api.controller;

import in.bored.api.dto.ContentFetchRequest;
import in.bored.api.dto.ContentItemResponse;
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

    @PostMapping("/next")
    public ResponseEntity<List<ContentItemResponse>> getNextContent(
            @RequestBody ContentFetchRequest request
    ) {
        List<ContentItemResponse> items =
                contentFeedService.fetchNextForCurrentUser(request);
        return ResponseEntity.ok(items);
    }
}