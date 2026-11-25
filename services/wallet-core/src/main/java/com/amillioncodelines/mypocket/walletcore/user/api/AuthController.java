package com.amillioncodelines.mypocket.walletcore.user.api;

import com.amillioncodelines.mypocket.walletcore.user.application.LoginService;
import com.amillioncodelines.mypocket.walletcore.user.application.RegistrationService;
import com.amillioncodelines.mypocket.walletcore.wallet.api.WalletResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
// Public endpoints: no bearer token in the API description.
@SecurityRequirements
@RequestMapping("/api/v1/auth")
class AuthController {

    private final RegistrationService registrationService;
    private final LoginService loginService;

    AuthController(RegistrationService registrationService, LoginService loginService) {
        this.registrationService = registrationService;
        this.loginService = loginService;
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var token = loginService.login(request.email(), request.password());
        return new LoginResponse(token.value(), "Bearer", token.expiresIn().toSeconds());
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
