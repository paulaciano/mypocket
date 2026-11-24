package com.amillioncodelines.mypocket.walletcore.user.api;

record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
