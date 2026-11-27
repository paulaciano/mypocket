package com.amillioncodelines.mypocket.walletcore.transfer.application;

import com.amillioncodelines.mypocket.walletcore.shared.domain.Currency;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** T5: maximum amount of one transfer, per currency. */
@ConfigurationProperties("mypocket.limits")
public record TransferLimits(Map<Currency, BigDecimal> perOperation) {

    public BigDecimal perOperation(Currency currency) {
        var limit = perOperation.get(currency);
        if (limit == null) {
            throw new IllegalStateException("No per-operation limit configured for " + currency);
        }
        return limit;
    }
}
