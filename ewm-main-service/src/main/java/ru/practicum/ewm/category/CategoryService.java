package ru.practicum.ewm.category;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Transactional
    public CategoryDto createCategory(NewCategoryDto dto) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name=" + dto.getName() + " already exists.");
        }
        Category category = categoryRepository.save(CategoryMapper.toCategory(dto));
        return CategoryMapper.toCategoryDto(category);
    }

    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found."));

        if (categoryRepository.existsByNameAndIdNot(dto.getName(), catId)) {
            throw new ConflictException("Category with name=" + dto.getName() + " already exists.");
        }

        category.setName(dto.getName());
        return CategoryMapper.toCategoryDto(category);
    }

    @Transactional
    public void deleteCategory(Long catId) {
        if (!categoryRepository.existsById(catId)) {
            throw new NotFoundException("Category with id=" + catId + " was not found.");
        }
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Category with id=" + catId + " is not empty.");
        }
        categoryRepository.deleteById(catId);
    }

    public List<CategoryDto> getCategories(int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        return categoryRepository.findAll(page).getContent().stream()
                .map(CategoryMapper::toCategoryDto)
                .toList();
    }

    public CategoryDto getCategoryById(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found."));
        return CategoryMapper.toCategoryDto(category);
    }
}