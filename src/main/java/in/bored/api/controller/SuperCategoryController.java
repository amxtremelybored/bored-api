package in.bored.api.controller;

import in.bored.api.model.SuperCategory;
import in.bored.api.service.SuperCategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/super-categories")
public class SuperCategoryController {

    private final SuperCategoryService superCategoryService;

    public SuperCategoryController(SuperCategoryService superCategoryService) {
        this.superCategoryService = superCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<SuperCategory>> getAllSuperCategories() {
        return ResponseEntity.ok(superCategoryService.getAllSuperCategories());
    }
}
