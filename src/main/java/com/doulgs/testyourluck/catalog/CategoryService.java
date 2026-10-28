package com.doulgs.testyourluck.catalog;

import com.doulgs.testyourluck.catalog.CatalogDtos.CategoryRequest;
import com.doulgs.testyourluck.catalog.CatalogDtos.CategoryResponse;
import com.doulgs.testyourluck.shared.util.Numbers;
import com.doulgs.testyourluck.shared.web.BusinessRuleException;
import com.doulgs.testyourluck.shared.web.ConflictException;
import com.doulgs.testyourluck.shared.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        List<Product> products = productRepository.findAllByOrderByCreatedAtAsc();
        int totalWeight = products.stream().mapToInt(Product::getWeight).sum();
        return categoryRepository.findAllByOrderByNameAsc().stream().map(c -> {
            List<Product> ps = products.stream().filter(p -> c.getId().equals(p.getCategoryId())).toList();
            int weight = ps.stream().mapToInt(Product::getWeight).sum();
            long qty = ps.stream().mapToLong(Product::getQuantity).sum();
            return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), ps.size(),
                    ps.stream().map(Product::getName).toList(), qty, Numbers.pct(weight, totalWeight));
        }).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        String name = req.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Categoria já existe");
        }
        Category c = new Category();
        apply(c, req);
        categoryRepository.save(c);
        return find(c.getId());
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest req) {
        Category c = get(id);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) {
            throw new ConflictException("Categoria já existe");
        }
        apply(c, req);
        return find(id);
    }

    @Transactional
    public void delete(UUID id) {
        Category c = get(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("Categoria possui produtos vinculados");
        }
        c.setDeletedAt(Instant.now());
    }

    public Category get(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
    }

    private CategoryResponse find(UUID id) {
        return list().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    private static void apply(Category c, CategoryRequest req) {
        c.setName(req.name().trim());
        c.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
    }
}
