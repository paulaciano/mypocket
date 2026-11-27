package com.amillioncodelines.mypocket.walletcore.transfer.application;

import com.amillioncodelines.mypocket.walletcore.idempotency.application.Idempotency;
import com.amillioncodelines.mypocket.walletcore.idempotency.application.IdempotencyKeyReusedException;
import com.amillioncodelines.mypocket.walletcore.idempotency.application.IdempotentOperation;
import com.amillioncodelines.mypocket.walletcore.ledger.application.CurrencyMismatchException;
import com.amillioncodelines.mypocket.walletcore.ledger.application.Ledger;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.TransactionType;
import com.amillioncodelines.mypocket.walletcore.rejection.application.RejectedOperations;
import com.amillioncodelines.mypocket.walletcore.rejection.domain.RejectedOperation;
import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import com.amillioncodelines.mypocket.walletcore.wallet.application.UnknownAccountNumberException;
import com.amillioncodelines.mypocket.walletcore.wallet.application.WalletCannotReceiveException;
import com.amillioncodelines.mypocket.walletcore.wallet.application.WalletNotFoundException;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Sends money between two customers' wallets (T1–T8), once per idempotency key (I1–I7). */
@Service
@EnableConfigurationProperties(TransferLimits.class)
public class Transfers {

    private static final Logger log = LoggerFactory.getLogger(Transfers.class);

    /** What identifies a transfer request (I4): parsed values, the amount with scale 2. */
    public record Command(
            UUID sourceWalletId, String destinationAccountNumber, BigDecimal amount, String description) {}

    private final Idempotency idempotency;
    private final Ledger ledger;
    private final WalletRepository wallets;
    private final TransferLimits limits;
    private final RejectedOperations rejectedOperations;

    Transfers(
            Idempotency idempotency,
            Ledger ledger,
            WalletRepository wallets,
            TransferLimits limits,
            RejectedOperations rejectedOperations) {
        this.idempotency = idempotency;
        this.ledger = ledger;
        this.wallets = wallets;
        this.limits = limits;
        this.rejectedOperations = rejectedOperations;
    }

    public Idempotency.Response send(UUID userId, String idempotencyKey, Command command) {
        try {
            return idempotency.execute(
                    userId,
                    idempotencyKey,
                    IdempotentOperation.TRANSFER,
                    command,
                    HttpStatus.CREATED,
                    () -> transfer(userId, command));
        } catch (IdempotencyKeyReusedException notABusinessRejection) {
            throw notABusinessRejection;
        } catch (ApiException rejection) {
            log.warn(
                    "Transfer rejected: reason={}, userId={}, sourceWalletId={}, destinationAccountNumber={}",
                    rejection.code(),
                    userId,
                    command.sourceWalletId(),
                    command.destinationAccountNumber());
            // The currency lookup runs inside record(), so its failure cannot replace the rejection (R1).
            rejectedOperations.record(now -> RejectedOperation.transfer(
                    rejection.code(),
                    userId,
                    command.sourceWalletId(),
                    command.destinationAccountNumber(),
                    command.amount(),
                    wallets.findByIdAndOwnerId(command.sourceWalletId(), userId)
                            .map(wallet -> wallet.currency())
                            .orElse(null),
                    now));
            throw rejection;
        }
    }

    /** Runs inside the idempotency transaction, so a rejection leaves nothing behind (T6, I6). */
    private TransferView transfer(UUID userId, Command command) {
        var requestedSource =
                wallets.findByIdAndOwnerId(command.sourceWalletId(), userId).orElseThrow(WalletNotFoundException::new);
        // Checked before looking at the destination, so a wallet that cannot send learns nothing about it.
        // Checked again below on the locked wallet, whose status is current.
        if (!requestedSource.status().canSend()) {
            throw new WalletCannotSendException();
        }
        var sourceId = requestedSource.id();
        var destinationId = wallets.findIdByAccountNumber(command.destinationAccountNumber())
                .orElseThrow(UnknownAccountNumberException::new);
        if (sourceId.equals(destinationId)) {
            throw new SameWalletException();
        }
        var locked = ledger.lockCustomerWallets(sourceId, destinationId);
        var source = locked.get(sourceId);
        var destination = locked.get(destinationId);
        if (!source.status().canSend()) {
            throw new WalletCannotSendException();
        }
        if (source.currency() != destination.currency()) {
            throw new CurrencyMismatchException();
        }
        if (!destination.status().canReceive()) {
            throw new WalletCannotReceiveException();
        }
        var money = new Money(command.amount(), source.currency());
        if (money.amount().compareTo(limits.perOperation(source.currency())) > 0) {
            throw new LimitExceededException();
        }
        var transaction = ledger.move(
                TransactionType.TRANSFER, money, null, command.description(), source.id(), destination.id());
        return view(
                transaction.id(),
                source.accountNumber(),
                destination.accountNumber(),
                money,
                command.description(),
                transaction.createdAt());
    }

    static TransferView view(
            UUID id,
            String sourceAccountNumber,
            String destinationAccountNumber,
            Money money,
            String description,
            Instant createdAt) {
        return new TransferView(
                id,
                sourceAccountNumber,
                destinationAccountNumber,
                money.currency(),
                money.amount().toPlainString(),
                description,
                createdAt);
    }
}
