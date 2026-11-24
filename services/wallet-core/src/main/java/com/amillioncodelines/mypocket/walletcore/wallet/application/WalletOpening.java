package com.amillioncodelines.mypocket.walletcore.wallet.application;

import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.AccountNumber;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import com.amillioncodelines.mypocket.walletcore.wallet.infrastructure.WalletRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletOpening {

    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 5;

    private final WalletRepository wallets;
    private final Clock clock;
    private final RandomGenerator random = new SecureRandom();

    WalletOpening(WalletRepository wallets, Clock clock) {
        this.wallets = wallets;
        this.clock = clock;
    }

    /** Opens one wallet per supported currency. Must run inside the caller's transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<Wallet> openCustomerWallets(UUID ownerId) {
        var now = clock.instant();
        return Arrays.stream(Currency.values())
                .map(currency -> wallets.save(Wallet.open(newAccountNumber(), ownerId, currency, now)))
                .toList();
    }

    private String newAccountNumber() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            var candidate = AccountNumber.generate(random);
            if (!wallets.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a free account number");
    }
}
