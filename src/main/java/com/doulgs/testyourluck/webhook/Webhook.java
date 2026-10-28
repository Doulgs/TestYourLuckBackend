package com.doulgs.testyourluck.webhook;

import com.doulgs.testyourluck.shared.tenant.BaseEntity;
import com.doulgs.testyourluck.webhook.WebhookEnums.Method;
import com.doulgs.testyourluck.webhook.WebhookEnums.Trigger;
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
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "webhooks")
@SQLRestriction("deleted_at is null")
public class Webhook extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 10)
    private Trigger trigger = Trigger.PLAY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Method method = Method.POST;

    @Column(nullable = false, length = 2000)
    private String url;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<KeyValue> headers = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<KeyValue> params = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_ids", nullable = false)
    private List<UUID> eventIds = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "dispatch_count", nullable = false)
    private int dispatchCount;

    @Column(name = "last_dispatch_at")
    private Instant lastDispatchAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
