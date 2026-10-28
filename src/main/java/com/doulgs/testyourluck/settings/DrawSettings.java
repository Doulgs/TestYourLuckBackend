package com.doulgs.testyourluck.settings;

import com.doulgs.testyourluck.settings.DrawSettingsEnums.Background;
import com.doulgs.testyourluck.settings.DrawSettingsEnums.GameStyle;
import com.doulgs.testyourluck.settings.DrawSettingsEnums.LogoMode;
import com.doulgs.testyourluck.settings.DrawSettingsEnums.MachineSize;
import com.doulgs.testyourluck.shared.tenant.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Configuração da máquina de sorteio: uma por tenant. Os valores iniciais são os padrões do design. */
@Getter
@Setter
@Entity
@Table(name = "draw_settings")
public class DrawSettings extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GameStyle style = GameStyle.SLOT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private MachineSize size = MachineSize.M;

    @Column(nullable = false)
    private int credits = 1;

    @Column(name = "primary_color", nullable = false, length = 7)
    private String primaryColor = "#F36ACB";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Background background = Background.PRETO;

    @Enumerated(EnumType.STRING)
    @Column(name = "logo_mode", nullable = false, length = 10)
    private LogoMode logoMode = LogoMode.DEFAULT;

    @Column(name = "logo_media_id")
    private UUID logoMediaId;

    @Column(nullable = false, length = 80)
    private String title = "Teste sua sorte";

    @Column(name = "win_message", nullable = false, length = 160)
    private String winMessage = "Retire seu brinde no balcão";

    @Column(name = "ask_email", nullable = false)
    private boolean askEmail = true;

    @Column(name = "ask_phone", nullable = false)
    private boolean askPhone = true;

    @Column(name = "ask_cpf", nullable = false)
    private boolean askCpf = true;

    @Column(nullable = false)
    private boolean lights = true;

    @Column(nullable = false)
    private boolean sound = true;

    @Column(name = "show_prizes", nullable = false)
    private boolean showPrizes = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_field_ids", nullable = false)
    private List<UUID> customFieldIds = new ArrayList<>();
}
