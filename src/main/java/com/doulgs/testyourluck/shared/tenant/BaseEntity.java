package com.doulgs.testyourluck.shared.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Colunas obrigatórias de toda tabela: id, tenant, tenant_owner, created_at e updated_at.
 * O tenant é preenchido e filtrado automaticamente pelo Hibernate via {@link TenantId}.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity implements Persistable<UUID> {

    @Id
    private UUID id = UUID.randomUUID();

    @TenantId
    @Column(nullable = false, updatable = false)
    private UUID tenant;

    @Column(name = "tenant_owner", nullable = false, updatable = false)
    private UUID tenantOwner;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    protected void assignTenantOwner() {
        if (tenantOwner == null) {
            tenantOwner = TenantContext.require().owner();
        }
    }

    @PostLoad
    @PostPersist
    protected void markNotNew() {
        isNew = false;
    }

    protected void assignId(UUID id) {
        this.id = id;
    }
}
