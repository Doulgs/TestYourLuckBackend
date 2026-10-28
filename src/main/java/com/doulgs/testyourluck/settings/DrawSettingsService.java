package com.doulgs.testyourluck.settings;

import com.doulgs.testyourluck.field.CustomFieldRepository;
import com.doulgs.testyourluck.media.MediaService;
import com.doulgs.testyourluck.settings.DrawSettingsDtos.DrawSettingsRequest;
import com.doulgs.testyourluck.settings.DrawSettingsDtos.DrawSettingsResponse;
import com.doulgs.testyourluck.shared.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DrawSettingsService {

    private final DrawSettingsRepository repository;
    private final CustomFieldRepository customFieldRepository;
    private final MediaService mediaService;

    /** Configuração do tenant ou os padrões do design, sem gravar. */
    @Transactional(readOnly = true)
    public DrawSettings current() {
        return repository.findFirstBy().orElseGet(DrawSettings::new);
    }

    @Transactional(readOnly = true)
    public DrawSettingsResponse get() {
        return toResponse(current());
    }

    @Transactional
    public DrawSettingsResponse save(DrawSettingsRequest req) {
        DrawSettings s = repository.findFirstBy().orElseGet(DrawSettings::new);
        List<UUID> fields = req.customFieldIds() == null ? List.of() : req.customFieldIds().stream().distinct().toList();
        for (UUID id : fields) {
            if (!customFieldRepository.existsById(id)) {
                throw new NotFoundException("Campo não encontrado");
            }
        }
        s.setStyle(req.style());
        s.setSize(req.size());
        s.setCredits(req.credits());
        s.setPrimaryColor(req.primaryColor().toUpperCase());
        s.setBackground(req.background());
        s.setLogoMode(req.logoMode());
        s.setLogoMediaId(mediaService.requireOwn(req.logoMediaId()));
        s.setTitle(req.title().trim());
        s.setWinMessage(req.winMessage().trim());
        s.setAskEmail(req.askEmail());
        s.setAskPhone(req.askPhone());
        s.setAskCpf(req.askCpf());
        s.setLights(req.lights());
        s.setSound(req.sound());
        s.setShowPrizes(req.showPrizes());
        s.setCustomFieldIds(new ArrayList<>(fields));
        repository.save(s);
        return toResponse(s);
    }

    /** Inclui um campo recém-criado nas perguntas exibidas pela máquina. */
    @Transactional
    public void includeCustomField(UUID fieldId) {
        DrawSettings s = repository.findFirstBy().orElseGet(DrawSettings::new);
        if (s.isNew()) {
            customFieldRepository.findAllByOrderByCreatedAtAsc().forEach(f -> s.getCustomFieldIds().add(f.getId()));
        }
        if (!s.getCustomFieldIds().contains(fieldId)) {
            List<UUID> ids = new ArrayList<>(s.getCustomFieldIds());
            ids.add(fieldId);
            s.setCustomFieldIds(ids);
        }
        repository.save(s);
    }

    private DrawSettingsResponse toResponse(DrawSettings s) {
        return new DrawSettingsResponse(s.getStyle(), s.getSize(), s.getCredits(), s.getPrimaryColor(),
                s.getBackground(), s.getLogoMode(), s.getLogoMediaId(), mediaService.url(s.getLogoMediaId()),
                s.getTitle(), s.getWinMessage(), s.isAskEmail(), s.isAskPhone(), s.isAskCpf(), s.isLights(),
                s.isSound(), s.isShowPrizes(), List.copyOf(s.getCustomFieldIds()));
    }
}
