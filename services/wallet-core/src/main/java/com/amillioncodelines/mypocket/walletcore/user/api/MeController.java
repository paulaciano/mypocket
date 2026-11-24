package com.amillioncodelines.mypocket.walletcore.user.api;

import com.amillioncodelines.mypocket.walletcore.shared.api.CurrentUser;
import com.amillioncodelines.mypocket.walletcore.user.application.UserQueries;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MeController {

    private final UserQueries userQueries;

    MeController(UserQueries userQueries) {
        this.userQueries = userQueries;
    }

    @GetMapping("/api/v1/me")
    MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        var user = userQueries.get(CurrentUser.id(jwt));
        return new MeResponse(user.id(), user.email(), user.role());
    }
}
