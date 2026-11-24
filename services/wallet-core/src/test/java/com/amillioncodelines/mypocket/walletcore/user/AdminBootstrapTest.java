package com.amillioncodelines.mypocket.walletcore.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.user.application.AdminBootstrap;
import com.amillioncodelines.mypocket.walletcore.user.application.AdminProperties;
import com.amillioncodelines.mypocket.walletcore.user.application.LoginService;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.user.domain.Role;
import com.amillioncodelines.mypocket.walletcore.user.infrastructure.UserRepository;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@IntegrationTest
class AdminBootstrapTest {

    @Autowired
    AdminBootstrap adminBootstrap;

    @Autowired
    UserRepository users;

    @Autowired
    WalletRepository wallets;

    @Autowired
    LoginService loginService;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    JwtDecoder jwtDecoder;

    @Test
    @DisplayName("U5: the administrator configured for the environment exists after startup and logs in as ADMIN")
    void configuredAdminExistsAtStartup() {
        var admin = users.findByEmail("admin@example.com").orElseThrow();
        assertThat(admin.role()).isEqualTo(Role.ADMIN);

        var token = loginService.login("admin@example.com", "admin-test-example");
        assertThat(jwtDecoder.decode(token.value()).getClaimAsString("role")).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("U5: creates the administrator once, with no wallets, and leaves it untouched on later starts")
    void createsAdminOnce() {
        var email = uniqueEmail();

        adminBootstrap.createAdminIfMissing(new AdminProperties(email, "first-password"));
        var created = users.findByEmail(email).orElseThrow();
        adminBootstrap.createAdminIfMissing(new AdminProperties(email, "second-password"));

        var afterSecondStart = users.findByEmail(email).orElseThrow();
        assertThat(afterSecondStart.role()).isEqualTo(Role.ADMIN);
        assertThat(afterSecondStart.passwordHash()).isEqualTo(created.passwordHash());
        assertThat(wallets.findByOwnerIdOrderByCurrency(created.id())).isEmpty();
    }

    @Test
    @DisplayName("U5: does nothing when no administrator is configured")
    void noAdminWhenNotConfigured() {
        var before = users.count();

        adminBootstrap.createAdminIfMissing(new AdminProperties("", ""));
        adminBootstrap.createAdminIfMissing(new AdminProperties(null, null));

        assertThat(users.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("U5: never promotes an existing customer to administrator")
    void doesNotPromoteCustomer() {
        var email = uniqueEmail();
        registrationService.registerCustomer(email, "secret-pass");

        adminBootstrap.createAdminIfMissing(new AdminProperties(email, "admin-password"));

        assertThat(users.findByEmail(email).orElseThrow().role()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("U5: refuses an incomplete or invalid administrator configuration")
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> adminBootstrap.createAdminIfMissing(new AdminProperties(uniqueEmail(), "")))
                .hasMessageContaining("MYPOCKET_ADMIN_PASSWORD");
        assertThatThrownBy(() -> adminBootstrap.createAdminIfMissing(new AdminProperties("", "admin-password")))
                .hasMessageContaining("MYPOCKET_ADMIN_EMAIL");
        assertThatThrownBy(() -> adminBootstrap.createAdminIfMissing(new AdminProperties(uniqueEmail(), "short")))
                .hasMessageContaining("between 8 and 64");
    }

    private static String uniqueEmail() {
        return "admin-" + UUID.randomUUID() + "@example.com";
    }
}
