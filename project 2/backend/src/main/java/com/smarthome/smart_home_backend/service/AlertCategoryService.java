package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.AlertCategory;
import com.smarthome.smart_home_backend.repository.AlertCategoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AlertCategoryService {

    private final AlertCategoryRepository categoryRepository;

    public AlertCategoryService(
            AlertCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<AlertCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<AlertCategory> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional
    public AlertCategory createCategory(AlertCategory category) {
        return categoryRepository.save(category);
    }

    @Transactional
    public AlertCategory updateCategory(
            Long id,
            AlertCategory details) {

        AlertCategory existing =
                categoryRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Alert category not found"
                                )
                        );

        existing.setCategoryName(
                details.getCategoryName()
        );

        existing.setDefaultSeverity(
                details.getDefaultSeverity()
        );

        return categoryRepository.save(existing);
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}