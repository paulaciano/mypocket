package com.amillioncodelines.mypocket.walletcore.user.api;

import com.amillioncodelines.mypocket.walletcore.user.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,

        @NotNull @Size(min = User.MIN_PASSWORD_LENGTH, max = User.MAX_PASSWORD_LENGTH)
        String password) {}
