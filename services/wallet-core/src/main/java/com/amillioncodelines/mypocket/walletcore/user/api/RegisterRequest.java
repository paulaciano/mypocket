package com.amillioncodelines.mypocket.walletcore.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(min = 8, max = 64) String password) {}
