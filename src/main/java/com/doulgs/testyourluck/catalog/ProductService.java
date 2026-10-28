package com.doulgs.testyourluck.catalog;

import com.doulgs.testyourluck.catalog.CatalogDtos.ProductRequest;
import com.doulgs.testyourluck.catalog.CatalogDtos.ProductResponse;
import com.doulgs.testyourluck.dashboard.DashboardCache;
import com.doulgs.testyourluck.draw.DrawRepository;
import com.doulgs.testyourluck.event.Event;
import com.doulgs.testyourluck.event.EventRepository;
import com.doulgs.testyourluck.media.MediaService;
import com.doulgs.testyourluck.shared.util.Numbers;
import com.doulgs.testyourluck.shared.web.ConflictException;
import com.doulgs.testyourluck.shared.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    /** Sequência de matizes do design, atribuída pela ordem de cadastro. */
    static final int[] HUES = {200, 150, 80, 280, 30, 250, 340, 110};

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductEventRepository productEventRepository;
    private final EventRepository eventRepository;
    private final DrawRepository drawRepository;
    private final DashboardCache dashboardCache;
    private final MediaService mediaService;

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        List<Product> products = productRepository.findAllByOrderByCreatedAtAsc();
        Map<UUID, String> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<UUID, Set<UUID>> links = productEventRepository.eventsByProduct();
        Map<UUID, Long> drawn = drawRepository.countByProduct();
        int totalWeight = products.stream().mapToInt(Product::getWeight).sum();
        return products.stream().map(p -> new ProductResponse(p.getId(), p.getName(), p.getShortCode(), p.getSku(),
                p.getCategoryId(), p.getCategoryId() == null ? null : categories.get(p.getCategoryId()),
                p.getWeight(), p.getMinStock(), p.getHue(), Numbers.pct(p.getWeight(), totalWeight),
                p.getQuantity(), StockStatus.of(p.getQuantity(), p.getMinStock()),
                links.getOrDefault(p.getId(), Set.of()).stream().sorted(Comparator.naturalOrder()).toList(),
                p.getImageMediaId(), mediaService.url(p.getImageMediaId()),
                drawn.getOrDefault(p.getId(), 0L))).toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        if (productRepository.existsBySkuIgnoreCase(req.sku().trim())) {
            throw new ConflictException("SKU já cadastrado");
        }
        Product p = new Product();
        p.setHue(HUES[(int) (productRepository.count() % HUES.length)]);
        apply(p, req);
        productRepository.save(p);
        // Novos produtos participam de todos os eventos quando nenhum vínculo é informado.
        Set<UUID> events = req.eventIds() == null
                ? eventRepository.findAll().stream().map(Event::getId).collect(Collectors.toSet())
                : req.eventIds();
        link(p.getId(), events);
        dashboardCache.evictCurrentTenant();
        return find(p.getId());
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest req) {
        Product p = get(id);
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(req.sku().trim(), id)) {
            throw new ConflictException("SKU já cadastrado");
        }
        apply(p, req);
        if (req.eventIds() != null) {
            productEventRepository.deleteByProductId(id);
            link(id, req.eventIds());
        }
        dashboardCache.evictCurrentTenant();
        return find(id);
    }

    /** Exclusão lógica: o produto sai do catálogo e do sorteio, mas permanece no histórico de leads. */
    @Transactional
    public void delete(UUID id) {
        get(id).setDeletedAt(Instant.now());
        dashboardCache.evictCurrentTenant();
    }

    public Product get(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Produto não encontrado"));
    }

    /** Iniciais das duas primeiras palavras; nome de uma palavra usa as duas primeiras letras. */
    static String initials(String name) {
        String[] words = name.trim().split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase();
        }
        return Arrays.stream(words).limit(2).map(w -> w.substring(0, 1)).collect(Collectors.joining()).toUpperCase();
    }

    private void link(UUID productId, Set<UUID> eventIds) {
        for (UUID eventId : eventIds) {
            if (!eventRepository.existsById(eventId)) {
                throw new NotFoundException("Evento não encontrado");
            }
            productEventRepository.save(new ProductEvent(productId, eventId));
        }
    }

    private ProductResponse find(UUID id) {
        productRepository.flush();
        return list().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    private void apply(Product p, ProductRequest req) {
        if (req.categoryId() != null && !categoryRepository.existsById(req.categoryId())) {
            throw new NotFoundException("Categoria não encontrada");
        }
        p.setName(req.name().trim());
        p.setSku(req.sku().trim());
        p.setCategoryId(req.categoryId());
        p.setWeight(req.weight());
        p.setMinStock(req.minStock());
        p.setQuantity(req.quantity());
        p.setImageMediaId(mediaService.requireOwn(req.imageMediaId()));
        p.setShortCode(req.shortCode() == null || req.shortCode().isBlank()
                ? initials(req.name()) : req.shortCode().trim().toUpperCase());
    }
}
