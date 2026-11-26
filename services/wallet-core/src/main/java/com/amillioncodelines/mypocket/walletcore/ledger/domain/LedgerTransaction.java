package com.amillioncodelines.mypocket.walletcore.ledger.domain;

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

/** A money movement. Never updated or deleted (L2). */
@Entity
@Immutable
@Table(name = "transactions")
public class LedgerTransaction {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerTransaction() {}

    public LedgerTransaction(TransactionType type, Money money, String externalReference, Instant createdAt) {
        this.type = type;
        this.currency = money.currency();
        this.amount = money.amount();
        this.externalReference = externalReference;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public TransactionType type() {
        return type;
    }

    public Money money() {
        return new Money(amount, currency);
    }

    public String externalReference() {
        return externalReference;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
