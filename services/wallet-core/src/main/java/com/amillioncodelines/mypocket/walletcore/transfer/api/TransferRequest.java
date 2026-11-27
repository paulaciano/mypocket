package com.amillioncodelines.mypocket.walletcore.transfer.api;

import com.amillioncodelines.mypocket.walletcore.shared.domain.AcceptableAmount;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

record TransferRequest(
        @NotNull UUID sourceWalletId,

        @NotNull @Pattern(regexp = "[0-9]{10}", message = "must have 10 digits")
        String destinationAccountNumber,

        @NotNull @AcceptableAmount String amount,
        // T8: shown to both sides. No Unicode control, format (bidi overrides) or line/paragraph separator
        // characters, so it cannot break a log line or disguise text in a client view.
        @Size(max = 140)
        @Pattern(
                regexp = "[^\\p{Cc}\\p{Cf}\\p{Zl}\\p{Zp}]*",
                message = "must not contain control or formatting characters")
        String description) {}
