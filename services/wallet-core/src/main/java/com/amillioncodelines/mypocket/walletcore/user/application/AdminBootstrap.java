package com.amillioncodelines.mypocket.walletcore.user.application;

import com.amillioncodelines.mypocket.walletcore.user.domain.Role;
import com.amillioncodelines.mypocket.walletcore.user.domain.User;
import com.amillioncodelines.mypocket.walletcore.user.infrastructure.UserRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the administrator account from configuration on startup, if it does not exist yet (U5). */
@Component
@EnableConfigurationProperties(AdminProperties.class)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    AdminBootstrap(AdminProperties properties, UserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        createAdminIfMissing(properties);
    }

    @Transactional
    public void createAdminIfMissing(AdminProperties admin) {
        if (!admin.isConfigured()) {
            return;
        }
        if (admin.email() == null || admin.email().isBlank() || !admin.email().contains("@")) {
            throw new IllegalStateException("MYPOCKET_ADMIN_EMAIL must be a valid email when the admin is configured");
        }
        if (!User.isAcceptablePassword(admin.password())) {
            throw new IllegalStateException("MYPOCKET_ADMIN_PASSWORD must have between " + User.MIN_PASSWORD_LENGTH
                    + " and " + User.MAX_PASSWORD_LENGTH + " characters");
        }
        var email = User.normalizeEmail(admin.email());
        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().role() != Role.ADMIN) {
                log.warn("The configured admin email belongs to a customer; it was not promoted to admin");
            }
            return;
        }
        users.save(User.admin(email, passwordEncoder.encode(admin.password()), clock.instant()));
        log.info("Administrator account created");
    }
}
