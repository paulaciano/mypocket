package com.amillioncodelines.mypocket.walletcore.idempotency.application;

import com.amillioncodelines.mypocket.walletcore.idempotency.infrastructure.IdempotencyRecords;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs a money-moving customer operation at most once per user and idempotency key (I2–I7, ADR-0005). The
 * record and the operation share one transaction, so a business rejection leaves no record (I6).
 */
@Service
public class Idempotency {

    /** I1: 1 to 100 visible ASCII characters. */
    public static final String KEY_PATTERN = "[\\x21-\\x7E]{1,100}";

    private static final String PRIMARY_KEY = "pk_idempotency_records";

    /** {@code body} is the JSON response; {@code replayed} is true when it is the stored original (I3). */
    public record Response(int status, String body, boolean replayed) {}

    private final IdempotencyRecords records;
    private final TransactionTemplate transactions;
    private final JsonMapper json;
    private final Clock clock;

    Idempotency(
            IdempotencyRecords records, PlatformTransactionManager transactionManager, JsonMapper json, Clock clock) {
        this.records = records;
        this.transactions = new TransactionTemplate(transactionManager);
        this.json = json;
        this.clock = clock;
    }

    /**
     * @param request what identifies the request (I4): the parsed, normalized command, so formatting differences
     *     in the original body do not matter
     * @param action the operation; runs in the same transaction as the record
     */
    public Response execute(
            UUID userId,
            String key,
            IdempotentOperation operation,
            Object request,
            HttpStatus successStatus,
            Supplier<?> action) {
        var fingerprint = fingerprint(operation, request);
        try {
            return transactions.execute(status -> {
                records.insert(userId, key, operation.name(), fingerprint, clock.instant());
                var body = json.writeValueAsString(action.get());
                records.complete(userId, key, successStatus.value(), body);
                return new Response(successStatus.value(), body, false);
            });
        } catch (DuplicateKeyException ex) {
            if (!String.valueOf(ex.getMessage()).contains(PRIMARY_KEY)) {
                throw ex;
            }
            // The key was used before, or by a concurrent request that has committed by now (I5).
            return transactions.execute(status -> replay(userId, key, operation, fingerprint));
        }
    }

    private Response replay(UUID userId, String key, IdempotentOperation operation, String fingerprint) {
        var stored = records.find(userId, key).orElseThrow();
        if (!stored.operation().equals(operation.name())
                || !stored.fingerprint().equals(fingerprint)) {
            throw new IdempotencyKeyReusedException();
        }
        return new Response(stored.responseStatus(), stored.responseBody(), true);
    }

    private String fingerprint(IdempotentOperation operation, Object request) {
        try {
            var canonical = operation.name() + ":" + json.writeValueAsString(request);
            var digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
