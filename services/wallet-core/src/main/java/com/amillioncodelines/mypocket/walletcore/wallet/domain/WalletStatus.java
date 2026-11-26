package com.amillioncodelines.mypocket.walletcore.wallet.domain;

public enum WalletStatus {
    ACTIVE,
    DEBIT_BLOCKED,
    FROZEN,
    CLOSED;

    /** W3: cash-ins and incoming transfers. */
    public boolean canReceive() {
        return this == ACTIVE || this == DEBIT_BLOCKED;
    }
}
