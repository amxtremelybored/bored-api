package in.bored.api.service;

import in.bored.api.model.SuperCategory;
import in.bored.api.repo.SuperCategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SuperCategoryService {

    private final SuperCategoryRepository superCategoryRepository;

    public SuperCategoryService(SuperCategoryRepository superCategoryRepository) {
        this.superCategoryRepository = superCategoryRepository;
    }

    public List<SuperCategory> getAllSuperCategories() {
        return superCategoryRepository.findAll();
    }
}
