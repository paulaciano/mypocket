package com.amillioncodelines.mypocket.walletcore.rejection.domain;

import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.UuidGenerator;

/** A money operation a business rule rejected (R1). Never updated or deleted (R2). */
@Entity
@Immutable
@Table(name = "rejected_operations")
public class RejectedOperation {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType type;

    @Column(nullable = false)
    private String reason;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "source_wallet_id")
    private UUID sourceWalletId;

    @Column(name = "destination_account_number")
    private String destinationAccountNumber;

    /** Unknown when the operation named a wallet that does not exist. */
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RejectedOperation() {}

    private RejectedOperation(
            OperationType type,
            String reason,
            UUID userId,
            String providerReference,
            UUID sourceWalletId,
            String destinationAccountNumber,
            BigDecimal amount,
            Currency currency,
            Instant createdAt) {
        this.type = type;
        this.reason = reason;
        this.userId = userId;
        this.providerReference = providerReference;
        this.sourceWalletId = sourceWalletId;
        this.destinationAccountNumber = destinationAccountNumber;
        this.currency = currency;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public static RejectedOperation cashIn(
            String reason, String providerReference, String accountNumber, Money money, Instant now) {
        return new RejectedOperation(
                OperationType.CASH_IN,
                reason,
                null,
                providerReference,
                null,
                accountNumber,
                money.amount(),
                money.currency(),
                now);
    }

    public static RejectedOperation transfer(
            String reason,
            UUID userId,
            UUID sourceWalletId,
            String destinationAccountNumber,
            BigDecimal amount,
            Currency currency,
            Instant now) {
        return new RejectedOperation(
                OperationType.TRANSFER,
                reason,
                userId,
                null,
                sourceWalletId,
                destinationAccountNumber,
                amount,
                currency,
                now);
    }

    public UUID id() {
        return id;
    }

    public OperationType type() {
        return type;
    }

    public String reason() {
        return reason;
    }

    public UUID userId() {
        return userId;
    }

    public String providerReference() {
        return providerReference;
    }

    public UUID sourceWalletId() {
        return sourceWalletId;
    }

    public String destinationAccountNumber() {
        return destinationAccountNumber;
    }

    public BigDecimal amount() {
        return amount;
    }

    public Currency currency() {
        return currency;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
