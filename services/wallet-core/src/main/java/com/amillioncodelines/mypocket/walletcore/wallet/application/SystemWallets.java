package com.amillioncodelines.mypocket.walletcore.wallet.application;

import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.SystemPurpose;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** The counterpart wallets of the platform, created by migration: one per purpose and currency. */
@Service
public class SystemWallets {

    private final WalletRepository wallets;

    SystemWallets(WalletRepository wallets) {
        this.wallets = wallets;
    }

    public UUID id(SystemPurpose purpose, Currency currency) {
        return wallets.findByPurposeAndCurrency(purpose, currency)
                .orElseThrow(() -> new IllegalStateException("Missing system wallet " + purpose + " " + currency))
                .id();
    }
}
