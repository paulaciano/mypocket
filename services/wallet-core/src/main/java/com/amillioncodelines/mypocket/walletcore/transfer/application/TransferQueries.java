package com.amillioncodelines.mypocket.walletcore.transfer.application;

import com.amillioncodelines.mypocket.walletcore.ledger.application.Ledger;
import com.amillioncodelines.mypocket.walletcore.ledger.domain.TransactionType;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferQueries {

    private final Ledger ledger;
    private final WalletRepository wallets;

    TransferQueries(Ledger ledger, WalletRepository wallets) {
        this.ledger = ledger;
        this.wallets = wallets;
    }

    /** Only the sender and the recipient see a transfer; anyone else is told it does not exist (S4). */
    @Transactional(readOnly = true)
    public TransferView transferOf(UUID userId, UUID transferId) {
        var posted = ledger.find(TransactionType.TRANSFER, transferId).orElseThrow(TransferNotFoundException::new);
        var source = wallet(posted.debitedWalletId());
        var destination = wallet(posted.creditedWalletId());
        if (!userId.equals(source.ownerId()) && !userId.equals(destination.ownerId())) {
            throw new TransferNotFoundException();
        }
        var transaction = posted.transaction();
        return Transfers.view(
                transaction.id(),
                source.accountNumber(),
                destination.accountNumber(),
                transaction.money(),
                transaction.description(),
                transaction.createdAt());
    }

    private Wallet wallet(UUID id) {
        return wallets.findById(id).orElseThrow();
    }
}
