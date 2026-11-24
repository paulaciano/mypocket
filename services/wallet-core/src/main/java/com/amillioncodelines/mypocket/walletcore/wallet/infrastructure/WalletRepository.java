package com.amillioncodelines.mypocket.walletcore.wallet.infrastructure;

import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    boolean existsByAccountNumber(String accountNumber);

    List<Wallet> findByOwnerIdOrderByCurrency(UUID ownerId);

    Optional<Wallet> findByIdAndOwnerId(UUID id, UUID ownerId);
}
