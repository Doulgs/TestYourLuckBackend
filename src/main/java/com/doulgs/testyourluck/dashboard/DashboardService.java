package com.doulgs.testyourluck.dashboard;

import com.doulgs.testyourluck.catalog.Product;
import com.doulgs.testyourluck.catalog.ProductRepository;
import com.doulgs.testyourluck.dashboard.DashboardDtos.ActiveEvent;
import com.doulgs.testyourluck.dashboard.DashboardDtos.DashboardResponse;
import com.doulgs.testyourluck.dashboard.DashboardDtos.DrawsChart;
import com.doulgs.testyourluck.dashboard.DashboardDtos.Kpi;
import com.doulgs.testyourluck.dashboard.DashboardDtos.Kpis;
import com.doulgs.testyourluck.dashboard.DashboardDtos.LeadsChart;
import com.doulgs.testyourluck.dashboard.DashboardDtos.LowStock;
import com.doulgs.testyourluck.dashboard.DashboardDtos.RecentDraw;
import com.doulgs.testyourluck.dashboard.DashboardDtos.Series;
import com.doulgs.testyourluck.dashboard.DashboardDtos.TopProduct;
import com.doulgs.testyourluck.draw.Draw;
import com.doulgs.testyourluck.draw.DrawRepository;
import com.doulgs.testyourluck.event.Event;
import com.doulgs.testyourluck.event.EventRepository;
import com.doulgs.testyourluck.event.EventStatus;
import com.doulgs.testyourluck.shared.tenant.TenantContext;
import com.doulgs.testyourluck.shared.util.Formats;
import com.doulgs.testyourluck.catalog.Category;
import com.doulgs.testyourluck.catalog.CategoryRepository;
import com.doulgs.testyourluck.catalog.ProductEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd/MM");

    /** Janela do gráfico: quantidade de pontos e dias por ponto, como no design. */
    record Window(int range, int points, int step, String label) {

        static Window of(int range) {
            return switch (range) {
                case 7 -> new Window(7, 7, 1, "Últimos 7 dias");
                case 90 -> new Window(90, 13, 7, "Últimos 3 meses");
                default -> new Window(30, 10, 3, "Últimos 30 dias");
            };
        }

        int days() {
            return points * step;
        }
    }

    private final DrawRepository drawRepository;
    private final EventRepository eventRepository;
    private final ProductRepository productRepository;
    private final ProductEventRepository productEventRepository;
    private final CategoryRepository categoryRepository;
    private final DashboardCache cache;
    private final TransactionTemplate tx;
    private final Clock clock = Clock.system(Formats.ZONE);

    public DashboardResponse get(int range) {
        Window w = Window.of(range);
        return cache.get(TenantContext.require().tenant(), w.range(), () -> tx.execute(s -> compute(w)));
    }

    DashboardResponse compute(Window w) {
        LocalDate today = LocalDate.now(clock);
        LocalDate firstDay = today.minusDays(w.days() - 1L);
        Instant start = firstDay.atStartOfDay(Formats.ZONE).toInstant();
        Instant prevStart = firstDay.minusDays(w.days()).atStartOfDay(Formats.ZONE).toInstant();

        List<Draw> window = drawRepository.findDrawnSince(prevStart);
        List<Draw> current = window.stream().filter(d -> !d.getDrawnAt().isBefore(start)).toList();
        List<Draw> previous = window.stream().filter(d -> d.getDrawnAt().isBefore(start)).toList();

        List<Event> events = eventRepository.findAll();
        List<Product> products = productRepository.findAllByOrderByCreatedAtAsc();
        Map<UUID, Long> drawsByEvent = drawRepository.countByEvent();
        Map<UUID, Long> drawsByProduct = drawRepository.countByProduct();
        long stockTotal = productRepository.totalQuantity();

        Kpis kpis = new Kpis(
                new Kpi(drawRepository.count(), change(current.size(), previous.size())),
                new Kpi(drawRepository.countDistinctLeads(),
                        change(distinctLeads(current), distinctLeads(previous))),
                new Kpi(stockTotal, stockTotal + current.size() == 0 ? null
                        : round(-100.0 * current.size() / (stockTotal + current.size()))));

        List<String> labels = new ArrayList<>();
        for (int i = 0; i < w.points(); i++) {
            labels.add(today.minusDays((long) (w.points() - 1 - i) * w.step()).format(LABEL));
        }
        List<Series> series = events.stream()
                .sorted(Comparator.comparing((Event e) -> drawsByEvent.getOrDefault(e.getId(), 0L)).reversed())
                .limit(2)
                .map(e -> new Series(e.getId(), e.getName(),
                        buckets(current.stream().filter(d -> d.getEventId().equals(e.getId())).toList(), today, w)))
                .toList();
        long seriesTotal = series.stream().flatMap(s -> s.values().stream()).mapToLong(Long::longValue).sum();
        List<Long> leadValues = buckets(current, today, w);

        List<RecentDraw> recent = drawRepository.findRecent(PageRequest.of(0, 5)).stream()
                .map(d -> new RecentDraw(d.getId(), d.getParticipant().getName(), d.getProductName(), d.getProductHue(),
                        d.getEventName(), d.getDrawnAt()))
                .toList();

        List<Event> active = events.stream().filter(e -> e.getStatus() == EventStatus.ATIVO).toList();
        Map<UUID, String> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<UUID, String> eventNames = events.stream().collect(Collectors.toMap(Event::getId, Event::getName));
        Map<UUID, Set<UUID>> links = productEventRepository.eventsByProduct();
        List<LowStock> lowStock = products.stream()
                .filter(p -> p.getQuantity() <= p.getMinStock())
                .map(p -> new LowStock(p.getName(), lowStockDetail(p, categories, eventNames, links), p.getQuantity(),
                        p.getMinStock()))
                .toList();

        List<TopProduct> top = products.stream()
                .map(p -> new TopProduct(p.getId(), p.getName(), p.getHue(), drawsByProduct.getOrDefault(p.getId(), 0L)))
                .sorted(Comparator.comparingLong(TopProduct::drawn).reversed())
                .limit(5)
                .toList();

        List<ActiveEvent> activeEvents = active.stream()
                .sorted(Comparator.comparing(Event::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(e -> new ActiveEvent(e.getId(), e.getName(), e.getStartDate(), drawsByEvent.getOrDefault(e.getId(), 0L)))
                .toList();

        return new DashboardResponse(w.range(), w.label(), kpis,
                new DrawsChart(labels, series, seriesTotal),
                new LeadsChart(labels, leadValues, leadValues.stream().mapToLong(Long::longValue).sum()),
                recent, lowStock.stream().limit(5).toList(), top, activeEvents);
    }

    /** "Categoria · todos os eventos" ou os nomes dos eventos vinculados, como no design. */
    static String lowStockDetail(Product p, Map<UUID, String> categories, Map<UUID, String> eventNames,
                                 Map<UUID, Set<UUID>> links) {
        String category = p.getCategoryId() == null ? "Sem categoria" : categories.getOrDefault(p.getCategoryId(), "Sem categoria");
        List<String> evs = links.getOrDefault(p.getId(), Set.of()).stream().map(eventNames::get)
                .filter(java.util.Objects::nonNull).sorted().toList();
        if (evs.isEmpty()) {
            return category;
        }
        return category + " · " + (evs.size() == eventNames.size() ? "todos os eventos" : String.join(", ", evs));
    }

    /** Distribui os sorteios em pontos de {@code step} dias; o último ponto termina hoje. */
    static List<Long> buckets(List<Draw> draws, LocalDate today, Window w) {
        long[] values = new long[w.points()];
        for (Draw d : draws) {
            long ago = ChronoUnit.DAYS.between(d.getDrawnAt().atZone(Formats.ZONE).toLocalDate(), today);
            int idx = (int) (w.points() - 1 - ago / w.step());
            if (ago >= 0 && idx >= 0) {
                values[idx]++;
            }
        }
        List<Long> out = new ArrayList<>(values.length);
        for (long v : values) {
            out.add(v);
        }
        return out;
    }

    static Double change(long current, long previous) {
        return previous == 0 ? null : round(100.0 * (current - previous) / previous);
    }

    private static long distinctLeads(List<Draw> draws) {
        return draws.stream().map(d -> d.getParticipant().getCpf() != null ? d.getParticipant().getCpf()
                : d.getParticipantId().toString()).distinct().count();
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
