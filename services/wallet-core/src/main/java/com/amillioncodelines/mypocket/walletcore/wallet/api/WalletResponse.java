package com.amillioncodelines.mypocket.walletcore.wallet.api;

import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.WalletStatus;
import java.util.UUID;

public record WalletResponse(UUID id, String accountNumber, Currency currency, WalletStatus status, String balance) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.id(),
                wallet.accountNumber(),
                wallet.currency(),
                wallet.status(),
                wallet.balance().toPlainString());
    }
}
