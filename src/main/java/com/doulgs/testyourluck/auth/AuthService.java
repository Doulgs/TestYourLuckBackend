package com.doulgs.testyourluck.auth;

import com.doulgs.testyourluck.auth.AuthDtos.AuthResponse;
import com.doulgs.testyourluck.auth.AuthDtos.LoginRequest;
import com.doulgs.testyourluck.auth.AuthDtos.SignupRequest;
import com.doulgs.testyourluck.auth.AuthDtos.UserResponse;
import com.doulgs.testyourluck.shared.tenant.TenantContext;
import com.doulgs.testyourluck.shared.web.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TransactionTemplate tx;

    /**
     * Cria o tenant da empresa e o usuário proprietário. A sessão Hibernate fixa o tenant ao abrir,
     * por isso a verificação de e-mail (sem tenant) e a gravação (com o novo tenant) usam transações distintas.
     */
    public AuthResponse signup(SignupRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("E-mail já cadastrado");
        }
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext.set(tenantId, userId);
        try {
            tx.executeWithoutResult(s -> {
                tenantRepository.save(new Tenant(tenantId, req.company().trim()));
                userRepository.save(new User(userId, req.name().trim(), email, passwordEncoder.encode(req.password())));
            });
        } finally {
            TenantContext.clear();
        }
        return respond(new AuthenticatedUser(userId, tenantId, userId, req.name().trim(), email, req.company().trim()));
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));
        String company = tenantRepository.findById(user.getTenant()).map(Tenant::getName).orElse("");
        return respond(new AuthenticatedUser(user.getId(), user.getTenant(), user.getTenantOwner(),
                user.getName(), user.getEmail(), company));
    }

    private AuthResponse respond(AuthenticatedUser user) {
        return new AuthResponse(jwtService.issue(user), UserResponse.of(user));
    }
}
