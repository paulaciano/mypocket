package com.amillioncodelines.mypocket.walletcore.wallet.application;

import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletQueries {

    private final WalletRepository wallets;

    WalletQueries(WalletRepository wallets) {
        this.wallets = wallets;
    }

    @Transactional(readOnly = true)
    public List<Wallet> walletsOf(UUID ownerId) {
        return wallets.findByOwnerIdOrderByCurrency(ownerId);
    }

    /** Another customer's wallet is reported as missing, so wallet ids cannot be probed (W10, S4). */
    @Transactional(readOnly = true)
    public Wallet walletOf(UUID ownerId, UUID walletId) {
        return wallets.findByIdAndOwnerId(walletId, ownerId).orElseThrow(WalletNotFoundException::new);
    }
}
