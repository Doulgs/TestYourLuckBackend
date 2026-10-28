package com.doulgs.testyourluck.field;

import com.doulgs.testyourluck.shared.tenant.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "custom_fields")
@SQLRestriction("deleted_at is null")
public class CustomField extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String label;

    @Column(name = "field_key", nullable = false, length = 32)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 10)
    private FieldType fieldType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<String> options = new ArrayList<>();

    @Column(length = 200)
    private String placeholder;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
