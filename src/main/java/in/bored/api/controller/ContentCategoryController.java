// src/main/java/in/bored/api/controller/ContentCategoryController.java
package in.bored.api.controller;

import in.bored.api.dto.ContentCategoryRequest;
import in.bored.api.dto.ContentCategoryResponse;
import in.bored.api.service.ContentCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/content-categories")
public class ContentCategoryController {

    private final ContentCategoryService service;

    public ContentCategoryController(ContentCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ContentCategoryResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContentCategoryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<ContentCategoryResponse> create(@RequestBody ContentCategoryRequest request) {
        ContentCategoryResponse created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContentCategoryResponse> update(@PathVariable UUID id,
                                                          @RequestBody ContentCategoryRequest request) {
        ContentCategoryResponse updated = service.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}