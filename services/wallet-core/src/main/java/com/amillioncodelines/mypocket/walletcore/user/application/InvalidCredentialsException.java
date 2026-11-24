package com.amillioncodelines.mypocket.walletcore.user.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials", "Email or password is wrong.");
    }
}
