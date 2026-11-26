package com.amillioncodelines.mypocket.walletcore.wallet.domain;

import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
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
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private WalletKind kind;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private SystemPurpose purpose;

    @Column(name = "account_number", updatable = false)
    private String accountNumber;

    @Column(name = "owner_id", updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    /** Cached balance of a customer wallet; {@code null} for system wallets (L5). */
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Wallet() {}

    private Wallet(String accountNumber, UUID ownerId, Currency currency, Instant createdAt) {
        this.kind = WalletKind.CUSTOMER;
        this.accountNumber = accountNumber;
        this.ownerId = ownerId;
        this.currency = currency;
        this.status = WalletStatus.ACTIVE;
        this.balance = BigDecimal.ZERO.setScale(2);
        this.createdAt = createdAt;
    }

    public static Wallet open(String accountNumber, UUID ownerId, Currency currency, Instant now) {
        return new Wallet(accountNumber, ownerId, currency, now);
    }

    /** Only the ledger changes balances, with the wallet row locked (ADR-0004). */
    public void credit(BigDecimal amount) {
        requireCustomer();
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        requireCustomer();
        if (!covers(amount)) {
            throw new IllegalStateException("Balance would become negative");
        }
        balance = balance.subtract(amount);
    }

    public boolean covers(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    private void requireCustomer() {
        if (kind != WalletKind.CUSTOMER) {
            throw new IllegalStateException("System wallets have no cached balance (L5)");
        }
    }

    public boolean isCustomer() {
        return kind == WalletKind.CUSTOMER;
    }

    public UUID id() {
        return id;
    }

    public WalletKind kind() {
        return kind;
    }

    public SystemPurpose purpose() {
        return purpose;
    }

    public String accountNumber() {
        return accountNumber;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public Currency currency() {
        return currency;
    }

    public WalletStatus status() {
        return status;
    }

    public BigDecimal balance() {
        return balance;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
