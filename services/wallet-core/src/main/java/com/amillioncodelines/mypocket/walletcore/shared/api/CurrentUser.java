package com.amillioncodelines.mypocket.walletcore.shared.api;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** The authenticated user, taken from the token subject (S18). */
public final class CurrentUser {

    private CurrentUser() {}

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
