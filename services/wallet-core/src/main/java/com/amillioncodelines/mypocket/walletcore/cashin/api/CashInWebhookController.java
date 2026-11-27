package com.amillioncodelines.mypocket.walletcore.cashin.api;

import com.amillioncodelines.mypocket.walletcore.cashin.application.CashIns;
import com.amillioncodelines.mypocket.walletcore.shared.domain.Money;
import com.amillioncodelines.mypocket.walletcore.shared.webhook.VerifiedWebhook;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated by the provider signature, verified before this controller runs (S5). */
@RestController
@SecurityRequirements
@RequestMapping("/api/v1/webhooks")
class CashInWebhookController {

    private final CashIns cashIns;

    CashInWebhookController(CashIns cashIns) {
        this.cashIns = cashIns;
    }

    @PostMapping("/cash-ins")
    @Parameter(name = "X-Provider-Timestamp", in = ParameterIn.HEADER, required = true, description = "Unix seconds")
    @Parameter(
            name = "X-Provider-Signature",
            in = ParameterIn.HEADER,
            required = true,
            description = "sha256= + hex HMAC-SHA256 of timestamp + \".\" + raw body")
    ResponseEntity<CashInResponse> receive(@Valid @RequestBody CashInRequest request, HttpServletRequest http) {
        VerifiedWebhook.require(http);
        var result = cashIns.receive(new CashIns.CashIn(
                request.reference(),
                request.accountNumber(),
                new Money(new BigDecimal(request.amount()), request.currency())));
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(CashInResponse.of(result));
    }
}
