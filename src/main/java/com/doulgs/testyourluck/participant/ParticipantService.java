package com.doulgs.testyourluck.participant;

import com.doulgs.testyourluck.draw.DrawRepository;
import com.doulgs.testyourluck.event.Event;
import com.doulgs.testyourluck.event.EventRules;
import com.doulgs.testyourluck.event.EventService;
import com.doulgs.testyourluck.field.CustomField;
import com.doulgs.testyourluck.field.CustomFieldRepository;
import com.doulgs.testyourluck.participant.ParticipantDtos.ParticipantRequest;
import com.doulgs.testyourluck.participant.ParticipantDtos.ParticipantResponse;
import com.doulgs.testyourluck.settings.DrawSettings;
import com.doulgs.testyourluck.settings.DrawSettingsService;
import com.doulgs.testyourluck.shared.tenant.TenantContext;
import com.doulgs.testyourluck.shared.web.BadRequestException;
import com.doulgs.testyourluck.webhook.WebhookTriggerEvents.LeadRegistered;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final ParticipantRepository repository;
    private final EventService eventService;
    private final DrawRepository drawRepository;
    private final DrawSettingsService settingsService;
    private final CustomFieldRepository customFieldRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock = Clock.systemUTC();

    /** Valida o cadastro conforme a configuração da máquina e libera os créditos configurados. */
    @Transactional
    public ParticipantResponse register(ParticipantRequest req) {
        Event event = eventService.find(req.eventId());
        EventRules.check(event, drawRepository.countByEventId(event.getId()), clock.instant());
        DrawSettings settings = settingsService.current();

        String name = req.name() == null ? "" : req.name().trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Informe seu nome.");
        }
        String email = null;
        if (settings.isAskEmail()) {
            email = req.email() == null ? "" : req.email().trim();
            if (!EMAIL.matcher(email).matches()) {
                throw new BadRequestException("Informe um e-mail válido.");
            }
        }
        String phone = null;
        if (settings.isAskPhone()) {
            phone = digits(req.phone());
            if (phone.length() < 10 || phone.length() > 11) {
                throw new BadRequestException("Informe um telefone válido.");
            }
        }
        String cpf = null;
        if (settings.isAskCpf()) {
            cpf = digits(req.cpf());
            if (cpf.length() != 11) {
                throw new BadRequestException("Informe um CPF válido.");
            }
        }

        Participant p = new Participant();
        p.setEventId(event.getId());
        p.setName(name);
        p.setEmail(email);
        p.setPhone(phone);
        p.setCpf(cpf);
        p.setAnswers(answers(settings, req.answers()));
        p.setCreditsGranted(settings.getCredits());
        repository.save(p);

        var t = TenantContext.require();
        events.publishEvent(new LeadRegistered(t.tenant(), t.owner(), p.getId()));
        return new ParticipantResponse(p.getId(), p.getName(), p.getCreditsGranted(), p.creditsRemaining());
    }

    /** Respostas apenas das perguntas ativas exibidas pela máquina, validadas pelo tipo. */
    private Map<String, String> answers(DrawSettings settings, Map<String, Object> raw) {
        Map<String, Object> in = raw == null ? Map.of() : raw;
        Map<String, String> out = new LinkedHashMap<>();
        for (CustomField f : customFieldRepository.findAllByOrderByCreatedAtAsc()) {
            if (!f.isActive() || !settings.getCustomFieldIds().contains(f.getId())) {
                continue;
            }
            Object value = in.get(f.getFieldKey());
            String v = value == null ? "" : String.valueOf(value).trim();
            switch (f.getFieldType()) {
                case BOOL -> {
                    out.put(f.getFieldKey(), v.equalsIgnoreCase("true") || v.equalsIgnoreCase("sim") ? "sim" : "nao");
                    continue;
                }
                default -> {
                    if (v.isEmpty()) {
                        if (f.isRequired()) {
                            throw new BadRequestException("Preencha: " + f.getLabel());
                        }
                        continue;
                    }
                }
            }
            switch (f.getFieldType()) {
                case NUMBER -> {
                    try {
                        new BigDecimal(v.replace(',', '.'));
                    } catch (NumberFormatException ex) {
                        throw new BadRequestException("Valor inválido para " + f.getLabel());
                    }
                }
                case DATE -> {
                    try {
                        LocalDate.parse(v);
                    } catch (DateTimeParseException ex) {
                        throw new BadRequestException("Data inválida para " + f.getLabel());
                    }
                }
                case SELECT -> {
                    if (!f.getOptions().contains(v)) {
                        throw new BadRequestException("Opção inválida para " + f.getLabel());
                    }
                }
                default -> {
                }
            }
            out.put(f.getFieldKey(), v);
        }
        return out;
    }

    private static String digits(String v) {
        return v == null ? "" : v.replaceAll("\\D", "");
    }
}
