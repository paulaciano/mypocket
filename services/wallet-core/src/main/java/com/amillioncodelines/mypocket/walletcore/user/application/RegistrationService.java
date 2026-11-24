package com.amillioncodelines.mypocket.walletcore.user.application;

import com.amillioncodelines.mypocket.walletcore.user.domain.User;
import com.amillioncodelines.mypocket.walletcore.user.infrastructure.UserRepository;
import com.amillioncodelines.mypocket.walletcore.wallet.application.WalletOpening;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import java.time.Clock;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserRepository users;
    private final WalletOpening walletOpening;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    RegistrationService(
            UserRepository users, WalletOpening walletOpening, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.walletOpening = walletOpening;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public record Registration(User user, List<Wallet> wallets) {}

    @Transactional
    public Registration registerCustomer(String email, String rawPassword) {
        var normalizedEmail = User.normalizeEmail(email);
        if (users.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }
        var user = User.customer(normalizedEmail, passwordEncoder.encode(rawPassword), clock.instant());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // A concurrent registration with the same email won the race after the existence check.
            if (isEmailUniqueViolation(ex)) {
                throw new EmailAlreadyRegisteredException();
            }
            throw ex;
        }
        return new Registration(user, walletOpening.openCustomerWallets(user.id()));
    }

    private static boolean isEmailUniqueViolation(DataIntegrityViolationException ex) {
        return ex.getCause() instanceof ConstraintViolationException cve
                && "uk_users_email".equals(cve.getConstraintName());
    }
}
