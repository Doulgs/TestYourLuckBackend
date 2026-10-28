package com.doulgs.testyourluck.event;

import com.doulgs.testyourluck.shared.tenant.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "events")
@SQLRestriction("deleted_at is null")
public class Event extends BaseEntity {

    public static final String DEFAULT_TIMEZONE = "America/Sao_Paulo";

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "all_day", nullable = false)
    private boolean allDay = true;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "allow_outside", nullable = false)
    private boolean allowOutside;

    @Column(length = 200)
    private String place;

    @Column(length = 120)
    private String city;

    @Column(length = 2)
    private String uf;

    @Column(length = 20)
    private String region;

    @Column(length = 9)
    private String cep;

    @Column(nullable = false, length = 40)
    private String timezone = DEFAULT_TIMEZONE;

    @Column(name = "max_draws", nullable = false)
    private int maxDraws;

    @Column(length = 255)
    private String website;

    @Column(name = "banner_media_id")
    private UUID bannerMediaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.RASCUNHO;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
