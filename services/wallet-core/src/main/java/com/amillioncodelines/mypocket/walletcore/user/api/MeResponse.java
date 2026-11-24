package com.amillioncodelines.mypocket.walletcore.user.api;

import com.amillioncodelines.mypocket.walletcore.user.domain.Role;
import java.util.UUID;

record MeResponse(UUID userId, String email, Role role) {}
