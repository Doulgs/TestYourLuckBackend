package com.doulgs.testyourluck.auth;

import com.doulgs.testyourluck.shared.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.security().jwt().secret()));
        this.ttl = Duration.ofMinutes(props.security().jwt().expirationMinutes());
    }

    public String issue(AuthenticatedUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.userId().toString())
                .claim("tenant", user.tenant().toString())
                .claim("tenantOwner", user.tenantOwner().toString())
                .claim("name", user.name())
                .claim("email", user.email())
                .claim("company", user.company())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(c.getSubject()),
                    UUID.fromString(c.get("tenant", String.class)),
                    UUID.fromString(c.get("tenantOwner", String.class)),
                    c.get("name", String.class),
                    c.get("email", String.class),
                    c.get("company", String.class)));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
