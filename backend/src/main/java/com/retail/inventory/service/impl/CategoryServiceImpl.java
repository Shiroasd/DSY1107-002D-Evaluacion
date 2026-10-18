package com.retail.inventory.service.impl;

import com.retail.inventory.domain.Category;
import com.retail.inventory.dto.CategoryRequestDto;
import com.retail.inventory.dto.CategoryResponseDto;
import com.retail.inventory.exception.BadRequestException;
import com.retail.inventory.exception.ResourceNotFoundException;
import com.retail.inventory.repository.CategoryRepository;
import com.retail.inventory.repository.ProductRepository;
import com.retail.inventory.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
        return mapToResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto requestDto) {
        if (categoryRepository.existsByNameIgnoreCase(requestDto.getName().trim())) {
            throw new BadRequestException("Ya existe una categoría con el nombre: " + requestDto.getName());
        }

        Category category = Category.builder()
                .name(requestDto.getName().trim())
                .description(requestDto.getDescription())
                .build();

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto requestDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(requestDto.getName().trim(), id)) {
            throw new BadRequestException("Ya existe otra categoría con el nombre: " + requestDto.getName());
        }

        category.setName(requestDto.getName().trim());
        category.setDescription(requestDto.getDescription());

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Categoría no encontrada con ID: " + id);
        }

        var productsInCategory = productRepository.findByCategoryId(id);
        if (!productsInCategory.isEmpty()) {
            throw new BadRequestException("No se puede eliminar la categoría porque contiene " + productsInCategory.size() + " productos asociados.");
        }

        categoryRepository.deleteById(id);
    }

    private CategoryResponseDto mapToResponse(Category category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
