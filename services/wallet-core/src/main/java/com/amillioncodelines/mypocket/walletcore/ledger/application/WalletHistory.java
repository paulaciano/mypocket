package com.amillioncodelines.mypocket.walletcore.ledger.application;

import com.amillioncodelines.mypocket.walletcore.ledger.infrastructure.HistoryRow;
import com.amillioncodelines.mypocket.walletcore.ledger.infrastructure.LedgerEntryRepository;
import com.amillioncodelines.mypocket.walletcore.wallet.application.WalletQueries;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletHistory {

    private final WalletQueries walletQueries;
    private final LedgerEntryRepository entries;

    WalletHistory(WalletQueries walletQueries, LedgerEntryRepository entries) {
        this.walletQueries = walletQueries;
        this.entries = entries;
    }

    /** L6, W10: only the owner sees a wallet's history; another customer's wallet answers as missing. */
    @Transactional(readOnly = true)
    public Page<HistoryRow> of(UUID ownerId, UUID walletId, int page, int size) {
        var wallet = walletQueries.walletOf(ownerId, walletId);
        return entries.history(wallet.id(), PageRequest.of(page, size));
    }
}
