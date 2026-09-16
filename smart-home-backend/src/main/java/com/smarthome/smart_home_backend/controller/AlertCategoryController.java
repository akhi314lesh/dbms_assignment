package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.AlertCategory;
import com.smarthome.smart_home_backend.service.AlertCategoryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alert-categories")
public class AlertCategoryController {

    private final AlertCategoryService categoryService;

    public AlertCategoryController(
            AlertCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<AlertCategory> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertCategory> getCategoryById(
            @PathVariable Long id) {

        return categoryService.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    @PostMapping
    public ResponseEntity<AlertCategory> createCategory(
            @RequestBody AlertCategory category) {

        return ResponseEntity.ok(
                categoryService.createCategory(category)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertCategory> updateCategory(
            @PathVariable Long id,
            @RequestBody AlertCategory details) {

        try {
            return ResponseEntity.ok(
                    categoryService.updateCategory(id, details)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id) {

        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}