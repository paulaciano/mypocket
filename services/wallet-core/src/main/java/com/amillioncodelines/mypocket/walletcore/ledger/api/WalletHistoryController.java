package com.amillioncodelines.mypocket.walletcore.ledger.api;

import com.amillioncodelines.mypocket.walletcore.ledger.application.WalletHistory;
import com.amillioncodelines.mypocket.walletcore.shared.api.CurrentUser;
import com.amillioncodelines.mypocket.walletcore.shared.api.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class WalletHistoryController {

    private final WalletHistory walletHistory;

    WalletHistoryController(WalletHistory walletHistory) {
        this.walletHistory = walletHistory;
    }

    @GetMapping("/api/v1/wallets/{walletId}/entries")
    PageResponse<EntryResponse> entries(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID walletId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.of(walletHistory.of(CurrentUser.id(jwt), walletId, page, size), EntryResponse::from);
    }
}
