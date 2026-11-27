package com.amillioncodelines.mypocket.walletcore.rejection.infrastructure;

import com.amillioncodelines.mypocket.walletcore.rejection.domain.RejectedOperation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Append-only: no update or delete methods (R2). */
public interface RejectedOperationRepository extends Repository<RejectedOperation, UUID> {

    RejectedOperation save(RejectedOperation operation);

    List<RejectedOperation> findByProviderReference(String providerReference);

    List<RejectedOperation> findByUserId(UUID userId);
}
