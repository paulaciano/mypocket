package com.amillioncodelines.mypocket.walletcore.user.application;

import com.amillioncodelines.mypocket.walletcore.user.domain.AcceptableEmail;
import com.amillioncodelines.mypocket.walletcore.user.domain.AcceptablePassword;
import com.amillioncodelines.mypocket.walletcore.user.domain.Role;
import com.amillioncodelines.mypocket.walletcore.user.domain.User;
import com.amillioncodelines.mypocket.walletcore.user.infrastructure.UserRepository;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Creates the administrator account from configuration on startup, if it does not exist yet (U5). */
@Component
@EnableConfigurationProperties(AdminProperties.class)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Validator validator;
    private final TransactionTemplate transactions;

    AdminBootstrap(
            AdminProperties properties,
            UserRepository users,
            PasswordEncoder passwordEncoder,
            Clock clock,
            Validator validator,
            PlatformTransactionManager transactionManager) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.validator = validator;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        createAdminIfMissing(properties);
    }

    public void createAdminIfMissing(AdminProperties admin) {
        if (!admin.isConfigured()) {
            return;
        }
        var violations = validator.validate(new AdminCredentials(admin.email(), admin.password()));
        if (!violations.isEmpty()) {
            var fields = violations.stream()
                    .map(violation -> "email".equals(violation.getPropertyPath().toString())
                            ? "MYPOCKET_ADMIN_EMAIL must be a valid email"
                            : "MYPOCKET_ADMIN_PASSWORD " + violation.getMessage())
                    .sorted()
                    .toList();
            throw new IllegalStateException("Invalid administrator configuration: " + String.join("; ", fields));
        }
        var email = User.normalizeEmail(admin.email());
        try {
            transactions.executeWithoutResult(status -> createIfMissing(email, admin.password()));
        } catch (DataIntegrityViolationException ex) {
            log.warn("The configured admin email was registered concurrently; the administrator was not created");
        }
    }

    private void createIfMissing(String email, String rawPassword) {
        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            if (existing.get().role() != Role.ADMIN) {
                log.warn("The configured admin email belongs to a customer; it was not promoted to admin");
            }
            return;
        }
        users.saveAndFlush(User.admin(email, passwordEncoder.encode(rawPassword), clock.instant()));
        log.info("Administrator account created");
    }

    record AdminCredentials(
            @NotBlank @Email @AcceptableEmail String email,
            @AcceptablePassword String password) {}
}
