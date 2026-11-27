package com.amillioncodelines.mypocket.walletcore.rejection.application;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.amillioncodelines.mypocket.walletcore.rejection.domain.RejectedOperation;
import com.amillioncodelines.mypocket.walletcore.rejection.infrastructure.RejectedOperationRepository;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class RejectedOperationsTest {

    @Test
    @DisplayName("R1: a failure while recording never replaces the original rejection")
    void recordingFailureDoesNotPropagate() {
        RejectedOperationRepository failing = new RejectedOperationRepository() {
            @Override
            public RejectedOperation save(RejectedOperation operation) {
                throw new IllegalStateException("database unavailable");
            }

            @Override
            public List<RejectedOperation> findByProviderReference(String providerReference) {
                return List.of();
            }

            @Override
            public List<RejectedOperation> findByUserId(UUID userId) {
                return List.of();
            }
        };
        var operations = new RejectedOperations(failing, new NoOpTransactionManager(), Clock.systemUTC());

        assertThatCode(() -> operations.record(
                        now -> RejectedOperation.cashIn("X", "PRV-1", "1000000009", Money.of("1", Currency.ARS), now)))
                .doesNotThrowAnyException();
    }

    private static final class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {}

        @Override
        protected void doCommit(DefaultTransactionStatus status) {}

        @Override
        protected void doRollback(DefaultTransactionStatus status) {}
    }
}
