package com.amillioncodelines.mypocket.walletcore.user.application;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends ApiException {

    public EmailAlreadyRegisteredException() {
        super(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_REGISTERED",
                "Email already registered",
                "An account with this email already exists.");
    }
}
