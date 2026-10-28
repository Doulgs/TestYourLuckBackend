package com.doulgs.testyourluck.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record SignupRequest(
            @NotBlank(message = "Informe seu nome") @Size(max = 160) String name,
            @NotBlank(message = "Informe a empresa") @Size(max = 160) String company,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 255) String email,
            @NotBlank(message = "Informe a senha") @Size(min = 8, max = 72, message = "A senha deve ter no mínimo 8 caracteres") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail") String email,
            @NotBlank(message = "Informe a senha") String password) {
    }

    public record UserResponse(UUID id, String name, String email, String company, String initials) {

        public static UserResponse of(AuthenticatedUser u) {
            return new UserResponse(u.userId(), u.name(), u.email(), u.company(), initials(u.name()));
        }

        static String initials(String name) {
            return Arrays.stream(name.trim().split("\\s+"))
                    .filter(w -> !w.isEmpty())
                    .limit(2)
                    .map(w -> w.substring(0, 1).toUpperCase())
                    .collect(Collectors.joining());
        }
    }

    public record AuthResponse(String token, UserResponse user) {
    }
}
