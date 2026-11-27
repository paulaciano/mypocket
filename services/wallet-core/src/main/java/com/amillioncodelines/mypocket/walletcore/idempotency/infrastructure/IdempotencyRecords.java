package com.amillioncodelines.mypocket.walletcore.idempotency.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyRecords {

    public record StoredRecord(String operation, String fingerprint, int responseStatus, String responseBody) {}

    private final JdbcTemplate jdbc;

    IdempotencyRecords(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Fails with a DuplicateKeyException if the key exists. If another transaction holding the same key is still
     * running, waits for it first (ADR-0005), so the stored response is complete when this fails.
     */
    public void insert(UUID userId, String key, String operation, String fingerprint, Instant now) {
        jdbc.update("""
                insert into idempotency_records (user_id, idempotency_key, operation, fingerprint, created_at)
                values (?, ?, ?, ?, ?)""", userId, key, operation, fingerprint, Timestamp.from(now));
    }

    public void complete(UUID userId, String key, int status, String body) {
        jdbc.update(
                "update idempotency_records set response_status = ?, response_body = ? "
                        + "where user_id = ? and idempotency_key = ?",
                status,
                body,
                userId,
                key);
    }

    public Optional<StoredRecord> find(UUID userId, String key) {
        return jdbc
                .query(
                        """
                        select operation, fingerprint, response_status, response_body
                        from idempotency_records
                        where user_id = ? and idempotency_key = ?""",
                        (rs, row) -> new StoredRecord(
                                rs.getString("operation"),
                                rs.getString("fingerprint"),
                                rs.getInt("response_status"),
                                rs.getString("response_body")),
                        userId,
                        key)
                .stream()
                .findFirst();
    }
}
