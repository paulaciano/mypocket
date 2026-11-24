package com.amillioncodelines.mypocket.walletcore.user.api;

import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.wallet.api.WalletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final RegistrationService registrationService;

    AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        var registration = registrationService.registerCustomer(request.email(), request.password());
        return new RegistrationResponse(
                registration.user().id(),
                registration.user().email(),
                registration.wallets().stream().map(WalletResponse::from).toList());
    }
}
