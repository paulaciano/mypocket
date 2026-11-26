package com.amillioncodelines.mypocket.walletcore.wallet.infrastructure;

import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.SystemPurpose;
import com.amillioncodelines.mypocket.walletcore.wallet.domain.Wallet;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    boolean existsByAccountNumber(String accountNumber);

    Optional<Wallet> findByAccountNumber(String accountNumber);

    List<Wallet> findByOwnerIdOrderByCurrency(UUID ownerId);

    Optional<Wallet> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<Wallet> findByPurposeAndCurrency(SystemPurpose purpose, Currency currency);

    /**
     * Locks the given customer wallets in ascending id order, in one query, so concurrent operations on the
     * same wallets never deadlock (ADR-0004). System wallets are not locked: they have no cached balance.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select w from Wallet w
            where w.id in :ids
              and w.kind = com.amillioncodelines.mypocket.walletcore.wallet.domain.WalletKind.CUSTOMER
            order by w.id""")
    List<Wallet> lockCustomerWallets(Collection<UUID> ids);
}
