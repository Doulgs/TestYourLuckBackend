package com.doulgs.testyourluck.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DrawSettingsRepository extends JpaRepository<DrawSettings, UUID> {

    /** O filtro @TenantId restringe ao tenant corrente; há no máximo uma linha. */
    Optional<DrawSettings> findFirstBy();
}
