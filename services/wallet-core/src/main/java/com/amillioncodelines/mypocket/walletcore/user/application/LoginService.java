package com.amillioncodelines.mypocket.walletcore.user.application;

import com.amillioncodelines.mypocket.walletcore.shared.config.JwtProperties;
import com.amillioncodelines.mypocket.walletcore.user.domain.User;
import com.amillioncodelines.mypocket.walletcore.user.infrastructure.UserRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration timeToLive;
    private final Clock clock;
    // Compared against when the email is unknown, so both failures take the same time (U4).
    private final String dummyHash;

    LoginService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            JwtProperties jwtProperties,
            Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.timeToLive = jwtProperties.timeToLive();
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    public record AccessToken(String value, Duration expiresIn) {}

    @Transactional(readOnly = true)
    public AccessToken login(String email, String rawPassword) {
        var user = users.findByEmail(User.normalizeEmail(email));
        var hash = user.map(User::passwordHash).orElse(dummyHash);
        var matches = passwordEncoder.matches(rawPassword, hash);
        if (user.isEmpty() || !matches) {
            throw new InvalidCredentialsException();
        }
        return issueToken(user.get());
    }

    private AccessToken issueToken(User user) {
        var now = clock.instant();
        var claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(now.plus(timeToLive))
                .claim("role", user.role().name())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
        return new AccessToken(token.getTokenValue(), timeToLive);
    }
}
