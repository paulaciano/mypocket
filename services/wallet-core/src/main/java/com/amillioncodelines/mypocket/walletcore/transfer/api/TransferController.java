package com.amillioncodelines.mypocket.walletcore.transfer.api;

import com.amillioncodelines.mypocket.walletcore.idempotency.application.Idempotency;
import com.amillioncodelines.mypocket.walletcore.shared.api.CurrentUser;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import com.amillioncodelines.mypocket.walletcore.transfer.application.TransferQueries;
import com.amillioncodelines.mypocket.walletcore.transfer.application.TransferView;
import com.amillioncodelines.mypocket.walletcore.transfer.application.Transfers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
class TransferController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    static final String REPLAYED = "Idempotent-Replayed";

    private final Transfers transfers;
    private final TransferQueries transferQueries;

    TransferController(Transfers transfers, TransferQueries transferQueries) {
        this.transfers = transfers;
        this.transferQueries = transferQueries;
    }

    @PostMapping
    ResponseEntity<String> send(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = IDEMPOTENCY_KEY, required = false)
                    @NotNull
                    @Pattern(regexp = Idempotency.KEY_PATTERN, message = "must have 1 to 100 visible ASCII characters")
                    String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        var description =
                request.description() == null || request.description().isBlank() ? null : request.description();
        var command = new Transfers.Command(
                request.sourceWalletId(),
                request.destinationAccountNumber(),
                new BigDecimal(request.amount()).setScale(Money.SCALE),
                description);
        var response = transfers.send(CurrentUser.id(jwt), idempotencyKey, command);
        var builder = ResponseEntity.status(response.status()).contentType(MediaType.APPLICATION_JSON);
        if (response.replayed()) {
            builder.header(REPLAYED, "true");
        }
        // The stored JSON is sent as is, so a replay is byte-for-byte the original response (I3).
        return builder.body(response.body());
    }

    @GetMapping("/{transferId}")
    TransferView transfer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID transferId) {
        return transferQueries.transferOf(CurrentUser.id(jwt), transferId);
    }
}
