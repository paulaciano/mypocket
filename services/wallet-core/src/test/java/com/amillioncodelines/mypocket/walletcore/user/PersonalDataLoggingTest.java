package com.amillioncodelines.mypocket.walletcore.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.amillioncodelines.mypocket.walletcore.IntegrationTest;
import com.amillioncodelines.mypocket.walletcore.user.application.EmailAlreadyRegisteredException;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
class PersonalDataLoggingTest {

    @Autowired
    RegistrationService registrationService;

    @Test
    @DisplayName("P1, S17: a duplicate-email race does not write the email to the logs")
    void duplicateEmailIsNotLogged(CapturedOutput output) throws Exception {
        var email = "race-" + UUID.randomUUID() + "@example.com";
        int attempts = 8;
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(attempts)) {
            var results = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < attempts; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        registrationService.registerCustomer(email, "secret-pass");
                        return true;
                    } catch (EmailAlreadyRegisteredException ex) {
                        return false;
                    }
                }));
            }
            start.countDown();
            for (var result : results) {
                result.get();
            }
        }

        assertThat(output.getAll()).doesNotContain(email);
    }
}
