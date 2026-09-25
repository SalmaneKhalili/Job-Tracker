package org.salmanekhalili.jobtrack.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.salmanekhalili.jobtrack.domain.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * No Spring context, no database: the signing key, the claims and the failure
 * modes are the whole contract of this class.
 */
class JwtServiceTest {

    /** HS256 needs at least 256 bits of key material. */
    private static final String SECRET = base64("unit-test-secret-key-long-enough-for-hs256");
    private static final String OTHER_SECRET = base64("a-completely-different-secret-key-value");

    private final JwtService jwtService = new JwtService(SECRET, 60_000);

    @Test
    void roundTripsTheUserIdFromTheSubjectClaim() {
        User user = user(42L, "you@example.com");

        String token = jwtService.genToken(user);

        assertThat(jwtService.parseUserID(token)).isEqualTo(42L);
    }

    @Test
    void carriesTheDocumentedClaims() {
        User user = user(42L, "you@example.com");

        String token = jwtService.genToken(user);
        Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("you@example.com");
        assertThat(claims.getExpiration()).isAfter(Date.from(Instant.now()));
    }

    @Test
    void rejectsATokenSignedWithAnotherKey() {
        String foreignToken = new JwtService(OTHER_SECRET, 60_000).genToken(user(42L, "you@example.com"));

        assertThatThrownBy(() -> jwtService.parseUserID(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsAnExpiredToken() {
        String expired = new JwtService(SECRET, -1_000).genToken(user(42L, "you@example.com"));

        assertThatThrownBy(() -> jwtService.parseUserID(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> jwtService.parseUserID("not-a-jwt"))
                .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }

    @Test
    void rejectsATokenWhoseSecretIsNotUsableKeyMaterial() {
        JwtService weakKeyService = new JwtService(base64("short"), 60_000);
        // The failure may surface at signing or at parsing time, depending on
        // which side trips over the key length first; either way it must not
        // hand back a usable token.
        assertThatThrownBy(() -> {
            String token = weakKeyService.genToken(user(1L, "you@example.com"));
            weakKeyService.parseUserID(token);
        }).isInstanceOf(RuntimeException.class);
    }

    private static User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private static String base64(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }
}
