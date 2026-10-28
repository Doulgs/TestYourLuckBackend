package com.doulgs.testyourluck.field;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class CustomFieldDtos {

    private CustomFieldDtos() {
    }

    public record CustomFieldRequest(
            @NotBlank(message = "Informe a pergunta") @Size(max = 160) String label,
            @Size(max = 64) String key,
            @NotNull(message = "Informe o tipo") FieldType type,
            List<String> options,
            @Size(max = 200) String placeholder,
            Boolean required,
            Boolean active) {
    }

    public record CustomFieldResponse(UUID id, String label, String key, String token, FieldType type,
                                      List<String> options, String placeholder, boolean required, boolean active) {

        static CustomFieldResponse of(CustomField f) {
            return new CustomFieldResponse(f.getId(), f.getLabel(), f.getFieldKey(), "{{" + f.getFieldKey() + "}}",
                    f.getFieldType(), f.getOptions(), f.getPlaceholder(), f.isRequired(), f.isActive());
        }
    }
}
