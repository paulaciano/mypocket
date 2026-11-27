package com.amillioncodelines.mypocket.walletcore.cashin.application;

import com.amillioncodelines.mypocket.walletcore.ledger.application.CurrencyMismatchException;
import com.amillioncodelines.mypocket.walletcore.ledger.application.Ledger;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.TransactionType;
import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import com.amillioncodelines.mypocket.walletcore.wallet.application.AccountNotFoundException;
import com.amillioncodelines.mypocket.walletcore.wallet.application.SystemWallets;
import com.amillioncodelines.mypocket.walletcore.wallet.application.WalletCannotReceiveException;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.SystemPurpose;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Applies cash-ins notified by the payment provider (CI1–CI7). */
@Service
public class CashIns {

    static final String REJECTED_METRIC = "mypocket.cashin.rejected";
    private static final String REFERENCE_CONSTRAINT = "uk_transactions_type_reference";
    private static final Logger log = LoggerFactory.getLogger(CashIns.class);

    private final Ledger ledger;
    private final WalletRepository wallets;
    private final SystemWallets systemWallets;
    private final TransactionTemplate transactions;
    private final MeterRegistry meters;

    CashIns(
            Ledger ledger,
            WalletRepository wallets,
            SystemWallets systemWallets,
            PlatformTransactionManager transactionManager,
            MeterRegistry meters) {
        this.ledger = ledger;
        this.wallets = wallets;
        this.systemWallets = systemWallets;
        this.transactions = new TransactionTemplate(transactionManager);
        this.meters = meters;
    }

    public record CashIn(String reference, String accountNumber, Money money) {}

    /** {@code created} is false when the reference was already applied with the same data (CI2). */
    public record Result(
            boolean created,
            UUID transactionId,
            String reference,
            String accountNumber,
            Money money,
            Instant createdAt) {}

    public Result receive(CashIn cashIn) {
        try {
            return applyOnce(cashIn);
        } catch (ApiException rejection) {
            // CI7: the provider keeps the money, so every rejection must be traceable.
            log.warn(
                    "Cash-in rejected: reason={}, reference={}, accountNumber={}",
                    rejection.code(),
                    cashIn.reference(),
                    cashIn.accountNumber());
            meters.counter(REJECTED_METRIC, "reason", rejection.code()).increment();
            throw rejection;
        }
    }

    private Result applyOnce(CashIn cashIn) {
        try {
            return transactions.execute(status -> apply(cashIn));
        } catch (DataIntegrityViolationException ex) {
            if (!isReferenceViolation(ex)) {
                throw ex;
            }
            // A concurrent delivery with the same reference was applied first.
            return transactions.execute(status -> alreadyApplied(cashIn).orElseThrow(() -> ex));
        }
    }

    private Result apply(CashIn cashIn) {
        var previous = alreadyApplied(cashIn);
        if (previous.isPresent()) {
            return previous.get();
        }
        // Locked from the first read: concurrent cash-ins to this wallet wait their turn and see its current state.
        var wallet = wallets.lockByAccountNumber(cashIn.accountNumber()).orElseThrow(AccountNotFoundException::new);
        if (wallet.currency() != cashIn.money().currency()) {
            throw new CurrencyMismatchException();
        }
        if (!wallet.status().canReceive()) {
            throw new WalletCannotReceiveException();
        }
        var external = systemWallets.id(SystemPurpose.EXTERNAL, wallet.currency());
        var transaction =
                ledger.move(TransactionType.CASH_IN, cashIn.money(), cashIn.reference(), external, wallet.id());
        return new Result(
                true,
                transaction.id(),
                cashIn.reference(),
                wallet.accountNumber(),
                transaction.money(),
                transaction.createdAt());
    }

    /** CI2, CI3: the same reference is applied once; with different data it is a conflict. */
    private Optional<Result> alreadyApplied(CashIn cashIn) {
        return ledger.findByReference(TransactionType.CASH_IN, cashIn.reference())
                .map(posted -> {
                    var transaction = posted.transaction();
                    var accountNumber = wallets.findById(posted.creditedWalletId())
                            .orElseThrow()
                            .accountNumber();
                    if (!accountNumber.equals(cashIn.accountNumber())
                            || !transaction.money().equals(cashIn.money())) {
                        throw new CashInReferenceConflictException();
                    }
                    return new Result(
                            false,
                            transaction.id(),
                            transaction.externalReference(),
                            accountNumber,
                            transaction.money(),
                            transaction.createdAt());
                });
    }

    private static boolean isReferenceViolation(DataIntegrityViolationException ex) {
        return ex.getCause() instanceof ConstraintViolationException cve
                && REFERENCE_CONSTRAINT.equals(cve.getConstraintName());
    }
}
