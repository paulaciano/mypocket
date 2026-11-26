package com.amillioncodelines.mypocket.walletcore.cashin.api;

import com.amillioncodelines.mypocket.walletcore.shared.domain.AcceptableAmount;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

record CashInRequest(
        // Provider identifiers only: no spaces or control characters, so they are safe to log (CI7).
        @NotNull @Pattern(regexp = "[A-Za-z0-9._:-]{1,100}", message = "must have 1 to 100 letters, digits or . _ : -")
        String reference,

        @NotNull @Pattern(regexp = "[0-9]{10}", message = "must have 10 digits")
        String accountNumber,

        @NotNull Currency currency,
        @NotNull @AcceptableAmount String amount) {}
