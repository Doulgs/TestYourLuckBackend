package com.doulgs.testyourluck.catalog;

import com.doulgs.testyourluck.shared.tenant.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "products")
@SQLRestriction("deleted_at is null")
public class Product extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "short_code", nullable = false, length = 2)
    private String shortCode;

    @Column(nullable = false, length = 60)
    private String sku;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false)
    private int weight;

    @Column(name = "min_stock", nullable = false)
    private int minStock;

    @Column(nullable = false)
    private int hue;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "image_media_id")
    private UUID imageMediaId;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
