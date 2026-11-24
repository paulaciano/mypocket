package com.amillioncodelines.mypocket.walletcore.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.user.application.EmailAlreadyRegisteredException;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.user.domain.Role;
import com.amillioncodelines.mypocket.walletcore.user.infrastructure.UserRepository;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.AccountNumber;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.WalletStatus;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class RegistrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository users;

    @Autowired
    WalletRepository wallets;

    @Autowired
    RegistrationService registrationService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("U3, W1, W2, M3: registering opens one ACTIVE wallet per currency with zero balance")
    void registersCustomerWithWallets() throws Exception {
        var email = uniqueEmail();

        register(email, "secret-pass")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.wallets", hasSize(2)))
                .andExpect(jsonPath("$.wallets[*].currency", containsInAnyOrder("ARS", "USD")))
                .andExpect(jsonPath("$.wallets[*].status", containsInAnyOrder("ACTIVE", "ACTIVE")))
                .andExpect(jsonPath("$.wallets[*].balance", containsInAnyOrder("0.00", "0.00")));

        var user = users.findAll().stream()
                .filter(u -> u.email().equals(email))
                .findFirst()
                .orElseThrow();
        assertThat(user.role()).isEqualTo(Role.CUSTOMER);
        var userWallets = wallets.findByOwnerIdOrderByCurrency(user.id());
        assertThat(userWallets).extracting(Wallet::currency).containsExactly(Currency.ARS, Currency.USD);
        assertThat(userWallets).extracting(Wallet::status).containsOnly(WalletStatus.ACTIVE);
        assertThat(userWallets)
                .allSatisfy(wallet -> assertThat(AccountNumber.isValid(wallet.accountNumber()))
                        .isTrue());
        assertThat(userWallets).extracting(Wallet::accountNumber).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("U1: email is stored in lower case")
    void normalizesEmail() throws Exception {
        var email = uniqueEmail();

        register(email.toUpperCase(), "secret-pass")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    @DisplayName("U1: rejects a missing or invalid email")
    void rejectsInvalidEmail() throws Exception {
        for (var email : new String[] {"", "not-an-email", "a@b@c", " spaced@example.com"}) {
            register(email, "secret-pass")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors[0].field").value("email"));
        }
    }

    @Test
    @DisplayName("U1: rejects an email already registered, ignoring case")
    void rejectsDuplicateEmail() throws Exception {
        var email = uniqueEmail();
        register(email, "secret-pass").andExpect(status().isCreated());

        register(email.toUpperCase(), "another-pass")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    @DisplayName("U1: concurrent registrations with the same email create a single user")
    void concurrentDuplicateRegistration() throws Exception {
        var email = uniqueEmail();
        int attempts = 8;
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(attempts)) {
            var results = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < attempts; i++) {
                Callable<Boolean> task = () -> {
                    start.await();
                    try {
                        registrationService.registerCustomer(email, "secret-pass");
                        return true;
                    } catch (EmailAlreadyRegisteredException ex) {
                        return false;
                    }
                };
                results.add(executor.submit(task));
            }
            start.countDown();
            long created = 0;
            for (var result : results) {
                if (result.get()) {
                    created++;
                }
            }
            assertThat(created).isEqualTo(1);
        }
        assertThat(users.existsByEmail(email)).isTrue();
    }

    @Test
    @DisplayName("U2: password must have between 8 and 64 characters")
    void validatesPasswordLength() throws Exception {
        register(uniqueEmail(), "a".repeat(7))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
        register(uniqueEmail(), "a".repeat(65)).andExpect(status().isBadRequest());
        register(uniqueEmail(), "a".repeat(8)).andExpect(status().isCreated());
        register(uniqueEmail(), "a".repeat(64)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("U2: the password is stored only as a BCrypt hash")
    void storesPasswordHash() throws Exception {
        var email = uniqueEmail();
        register(email, "secret-pass").andExpect(status().isCreated());

        var user = users.findAll().stream()
                .filter(u -> u.email().equals(email))
                .findFirst()
                .orElseThrow();
        assertThat(user.passwordHash()).startsWith("$2").isNotEqualTo("secret-pass");
        assertThat(passwordEncoder.matches("secret-pass", user.passwordHash())).isTrue();
    }

    @Test
    @DisplayName("P1, S2: the registration response exposes no password or hash")
    void responseHasNoSecrets() throws Exception {
        register(uniqueEmail(), "secret-pass")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("secret-pass"))));
    }

    @Test
    @DisplayName("W1: the database rejects a second wallet of the same currency for a customer")
    void oneWalletPerCurrency() throws Exception {
        var email = uniqueEmail();
        var registration = registrationService.registerCustomer(email, "secret-pass");
        var duplicate = Wallet.open("1000000009", registration.user().id(), Currency.ARS, Instant.now());

        assertThatThrownBy(() -> wallets.saveAndFlush(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("S7: rejects unknown fields and malformed JSON")
    void rejectsUnknownFieldsAndMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "secret-pass", "role": "ADMIN"}
                                """.formatted(uniqueEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("S9: errors are Problem Details without internal information")
    void errorsAreProblemDetails() throws Exception {
        register("not-an-email", "secret-pass")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    @DisplayName("S1: endpoints other than registration and health require authentication")
    void otherEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/wallets"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    private ResultActions register(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
