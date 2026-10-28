package com.doulgs.testyourluck.participant;

import com.doulgs.testyourluck.shared.tenant.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Lead cadastrado no totem; libera créditos de jogada controlados pelo servidor. */
@Getter
@Setter
@Entity
@Table(name = "participants")
public class Participant extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column
    private String email;

    @Column(length = 11)
    private String phone;

    @Column(length = 11)
    private String cpf;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, String> answers = new LinkedHashMap<>();

    @Column(name = "credits_granted", nullable = false)
    private int creditsGranted;

    @Column(name = "credits_used", nullable = false)
    private int creditsUsed;

    public int creditsRemaining() {
        return creditsGranted - creditsUsed;
    }
}
