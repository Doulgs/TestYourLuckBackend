package com.doulgs.testyourluck.webhook;

import com.doulgs.testyourluck.shared.tenant.BaseEntity;
import com.doulgs.testyourluck.webhook.WebhookEnums.Trigger;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Registro de um disparo de webhook (request resolvido e response/erro). */
@Getter
@Setter
@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery extends BaseEntity {

    @Column(name = "webhook_id", nullable = false)
    private UUID webhookId;

    @Column(name = "webhook_name", nullable = false, length = 160)
    private String webhookName;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 10)
    private Trigger trigger;

    @Column(nullable = false)
    private boolean test;

    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "draw_id")
    private UUID drawId;

    @Column(name = "event_name", length = 160)
    private String eventName;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, length = 4000)
    private String url;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> request;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> response;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(nullable = false)
    private boolean ok;

    @Column(length = 500)
    private String error;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
}
