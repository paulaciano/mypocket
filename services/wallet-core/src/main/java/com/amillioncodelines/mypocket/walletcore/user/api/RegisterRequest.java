package com.amillioncodelines.mypocket.walletcore.user.api;

import com.amillioncodelines.mypocket.walletcore.user.domain.AcceptableEmail;
import com.amillioncodelines.mypocket.walletcore.user.domain.AcceptablePassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

record RegisterRequest(
        @NotBlank @Email @AcceptableEmail String email,
        @AcceptablePassword String password) {}
