package com.doulgs.testyourluck.webhook;

import com.doulgs.testyourluck.field.CustomField;
import com.doulgs.testyourluck.settings.DrawSettingsEnums.GameStyle;
import com.doulgs.testyourluck.shared.util.Formats;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Monta o mapa de variáveis disponíveis nos templates dos webhooks. */
public final class WebhookVariables {

    private WebhookVariables() {
    }

    public record Lead(String name, String email, String phone, String cpf, Map<String, String> answers) {
    }

    public record EventData(UUID id, String name, String place) {
    }

    public record Prize(String name, String sku, String category) {
    }

    static final Map<GameStyle, String> STYLE = Map.of(GameStyle.SLOT, "caca-niquel", GameStyle.WHEEL, "roleta",
            GameStyle.SLIDER, "slider", GameStyle.BLOCKS, "blocos");

    public static Map<String, String> build(Lead lead, List<CustomField> fields, EventData event, Prize prize,
                                            GameStyle style, Instant at, boolean sample) {
        Map<String, String> m = new HashMap<>();
        m.put("lead.nome", lead.name());
        m.put("lead.email", lead.email());
        m.put("lead.telefone", lead.phone() == null ? null : Formats.phone(lead.phone()));
        m.put("lead.cpf", lead.cpf() == null ? null : Formats.cpf(lead.cpf()));
        for (CustomField f : fields) {
            String v = lead.answers() == null ? null : lead.answers().get(f.getFieldKey());
            if (v == null && sample) {
                v = "(" + f.getLabel() + ")";
            }
            m.put(f.getFieldKey(), v);
            m.put("campo." + f.getFieldKey(), v);
        }
        if (event != null) {
            m.put("evento.id", event.id() == null ? null : event.id().toString());
            m.put("evento.nome", event.name());
            m.put("evento.local", event.place());
        }
        if (prize != null) {
            m.put("premio.nome", prize.name());
            m.put("premio.sku", prize.sku());
            m.put("premio.categoria", prize.category());
        }
        m.put("jogo.estilo", STYLE.get(style));
        m.put("jogo.data_hora", at.toString());
        return m;
    }
}
