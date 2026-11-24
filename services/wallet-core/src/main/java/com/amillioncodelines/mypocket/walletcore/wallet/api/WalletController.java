package com.amillioncodelines.mypocket.walletcore.wallet.api;

import com.amillioncodelines.mypocket.walletcore.shared.api.CurrentUser;
import com.amillioncodelines.mypocket.walletcore.wallet.application.WalletQueries;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
class WalletController {

    private final WalletQueries walletQueries;

    WalletController(WalletQueries walletQueries) {
        this.walletQueries = walletQueries;
    }

    @GetMapping
    List<WalletResponse> myWallets(@AuthenticationPrincipal Jwt jwt) {
        return walletQueries.walletsOf(CurrentUser.id(jwt)).stream()
                .map(WalletResponse::from)
                .toList();
    }

    @GetMapping("/{walletId}")
    WalletResponse myWallet(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID walletId) {
        return WalletResponse.from(walletQueries.walletOf(CurrentUser.id(jwt), walletId));
    }
}
