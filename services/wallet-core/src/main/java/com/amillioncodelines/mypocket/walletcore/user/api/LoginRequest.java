package com.amillioncodelines.mypocket.walletcore.user.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record LoginRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(max = 64) String password) {}
