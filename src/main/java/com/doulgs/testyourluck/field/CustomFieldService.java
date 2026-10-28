package com.doulgs.testyourluck.field;

import com.doulgs.testyourluck.field.CustomFieldDtos.CustomFieldRequest;
import com.doulgs.testyourluck.field.CustomFieldDtos.CustomFieldResponse;
import com.doulgs.testyourluck.settings.DrawSettingsService;
import com.doulgs.testyourluck.shared.web.BadRequestException;
import com.doulgs.testyourluck.shared.web.ConflictException;
import com.doulgs.testyourluck.shared.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomFieldService {

    /** Prefixos usados pelas variáveis nativas dos webhooks. */
    static final List<String> RESERVED = List.of("lead", "evento", "premio", "jogo", "campo");

    private final CustomFieldRepository repository;
    private final DrawSettingsService drawSettingsService;

    @Transactional(readOnly = true)
    public List<CustomFieldResponse> list() {
        return repository.findAllByOrderByCreatedAtAsc().stream().map(CustomFieldResponse::of).toList();
    }

    /** Novos campos nascem ativos e já entram nas perguntas exibidas pela máquina. */
    @Transactional
    public CustomFieldResponse create(CustomFieldRequest req) {
        CustomField f = new CustomField();
        apply(f, req, null);
        repository.save(f);
        drawSettingsService.includeCustomField(f.getId());
        return CustomFieldResponse.of(f);
    }

    @Transactional
    public CustomFieldResponse update(UUID id, CustomFieldRequest req) {
        CustomField f = get(id);
        apply(f, req, id);
        return CustomFieldResponse.of(f);
    }

    /** Exclusão lógica: respostas já registradas continuam nos participantes. */
    @Transactional
    public void delete(UUID id) {
        get(id).setDeletedAt(Instant.now());
    }

    public CustomField get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Campo não encontrado"));
    }

    static String slug(String v) {
        String s = Normalizer.normalize(v == null ? "" : v, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        return s.length() > 32 ? s.substring(0, 32).replaceAll("_$", "") : s;
    }

    private void apply(CustomField f, CustomFieldRequest req, UUID id) {
        String key = slug(req.key() == null || req.key().isBlank() ? req.label() : req.key());
        if (key.isEmpty()) {
            throw new BadRequestException("Informe a chave da variável");
        }
        if (RESERVED.stream().anyMatch(key::startsWith)) {
            throw new BadRequestException("Chave reservada");
        }
        boolean duplicated = id == null ? repository.existsByFieldKey(key) : repository.existsByFieldKeyAndIdNot(key, id);
        if (duplicated) {
            throw new ConflictException("Chave já utilizada");
        }
        List<String> options = req.options() == null ? List.of()
                : req.options().stream().filter(o -> o != null && !o.isBlank()).map(String::trim).distinct().toList();
        if (req.type() == FieldType.SELECT && options.isEmpty()) {
            throw new BadRequestException("Informe ao menos uma opção");
        }
        f.setLabel(req.label().trim());
        f.setFieldKey(key);
        f.setFieldType(req.type());
        f.setOptions(req.type() == FieldType.SELECT ? options : List.of());
        f.setPlaceholder(req.placeholder() == null || req.placeholder().isBlank() ? null : req.placeholder().trim());
        f.setRequired(Boolean.TRUE.equals(req.required()));
        f.setActive(req.active() == null || req.active());
    }
}
