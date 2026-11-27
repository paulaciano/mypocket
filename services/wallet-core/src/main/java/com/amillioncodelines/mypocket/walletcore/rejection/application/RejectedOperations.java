package com.amillioncodelines.mypocket.walletcore.rejection.application;

import com.amillioncodelines.mypocket.walletcore.rejection.domain.RejectedOperation;
import com.amillioncodelines.mypocket.walletcore.rejection.infrastructure.RejectedOperationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Records money operations rejected by a business rule (R1). */
@Service
public class RejectedOperations {

    private static final Logger log = LoggerFactory.getLogger(RejectedOperations.class);

    private final RejectedOperationRepository operations;
    private final TransactionTemplate ownTransaction;
    private final Clock clock;

    RejectedOperations(
            RejectedOperationRepository operations, PlatformTransactionManager transactionManager, Clock clock) {
        this.operations = operations;
        this.ownTransaction = new TransactionTemplate(transactionManager);
        // The rejected operation rolls back its own transaction; the record must outlive it.
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    /**
     * Never throws: if the record cannot be written, the caller still answers with the original rejection and
     * the failure is logged.
     */
    public void record(Function<Instant, RejectedOperation> operation) {
        try {
            ownTransaction.executeWithoutResult(
                    status -> operations.save(operation.apply(clock.instant().truncatedTo(ChronoUnit.MICROS))));
        } catch (RuntimeException e) {
            log.error("Could not record a rejected operation", e);
        }
    }
}
