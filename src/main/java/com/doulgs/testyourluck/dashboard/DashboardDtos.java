package com.doulgs.testyourluck.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    public record Kpi(long value, Double changePct) {
    }

    public record Kpis(Kpi draws, Kpi leads, Kpi stock) {
    }

    public record Series(UUID eventId, String eventName, List<Long> values) {
    }

    public record DrawsChart(List<String> labels, List<Series> series, long total) {
    }

    public record LeadsChart(List<String> labels, List<Long> values, long total) {
    }

    public record RecentDraw(UUID id, String name, String productName, int productHue, String eventName, Instant drawnAt) {
    }

    public record LowStock(String productName, String detail, int quantity, int minStock) {
    }

    public record TopProduct(UUID productId, String name, int hue, long drawn) {
    }

    public record ActiveEvent(UUID id, String name, LocalDate date, long draws) {
    }

    public record DashboardResponse(int range, String label, Kpis kpis, DrawsChart drawsChart, LeadsChart leadsChart,
                                    List<RecentDraw> recent, List<LowStock> lowStock, List<TopProduct> topProducts,
                                    List<ActiveEvent> activeEvents) {
    }
}
