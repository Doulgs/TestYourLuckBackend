package com.doulgs.testyourluck.auth;

import com.doulgs.testyourluck.shared.tenant.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Empresa cliente. O próprio registro pertence ao tenant que representa (tenant = id). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    public Tenant(UUID id, String name) {
        assignId(id);
        this.name = name;
    }
}
