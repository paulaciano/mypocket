package com.amillioncodelines.mypocket.walletcore.user.api;

import com.amillioncodelines.mypocket.walletcore.wallet.api.WalletResponse;
import java.util.List;
import java.util.UUID;

record RegistrationResponse(UUID userId, String email, List<WalletResponse> wallets) {}
