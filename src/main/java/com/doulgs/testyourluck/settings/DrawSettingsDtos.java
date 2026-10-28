package com.doulgs.testyourluck.settings;

import com.doulgs.testyourluck.settings.DrawSettingsEnums.Background;
import com.doulgs.testyourluck.settings.DrawSettingsEnums.GameStyle;
import com.doulgs.testyourluck.settings.DrawSettingsEnums.LogoMode;
import com.doulgs.testyourluck.settings.DrawSettingsEnums.MachineSize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class DrawSettingsDtos {

    private DrawSettingsDtos() {
    }

    public record DrawSettingsRequest(
            @NotNull GameStyle style,
            @NotNull MachineSize size,
            @NotNull @Min(value = 1, message = "Créditos entre 1 e 5") @Max(value = 5, message = "Créditos entre 1 e 5") Integer credits,
            @NotNull @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Cor inválida") String primaryColor,
            @NotNull Background background,
            @NotNull LogoMode logoMode,
            UUID logoMediaId,
            @NotBlank(message = "Informe o título") @Size(max = 80) String title,
            @NotBlank(message = "Informe a mensagem do prêmio") @Size(max = 160) String winMessage,
            boolean askEmail,
            boolean askPhone,
            boolean askCpf,
            boolean lights,
            boolean sound,
            boolean showPrizes,
            List<UUID> customFieldIds) {
    }

    public record DrawSettingsResponse(GameStyle style, MachineSize size, int credits, String primaryColor,
                                       Background background, LogoMode logoMode, UUID logoMediaId, String logoUrl,
                                       String title, String winMessage, boolean askEmail, boolean askPhone,
                                       boolean askCpf, boolean lights, boolean sound, boolean showPrizes,
                                       List<UUID> customFieldIds) {
    }
}
