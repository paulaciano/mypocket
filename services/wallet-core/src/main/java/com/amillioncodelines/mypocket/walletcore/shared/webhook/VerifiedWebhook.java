package com.amillioncodelines.mypocket.walletcore.shared.webhook;

import com.amillioncodelines.mypocket.walletcore.shared.api.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

/**
 * Second line of defense (S5): webhook controllers only act on requests the signature filter marked as
 * verified, whatever path matching let them through.
 */
public final class VerifiedWebhook {

    static final String ATTRIBUTE = VerifiedWebhook.class.getName();

    private VerifiedWebhook() {}

    public static void require(HttpServletRequest request) {
        if (!Boolean.TRUE.equals(request.getAttribute(ATTRIBUTE))) {
            throw new InvalidSignatureException();
        }
    }

    static void mark(HttpServletRequest request) {
        request.setAttribute(ATTRIBUTE, Boolean.TRUE);
    }

    static final class InvalidSignatureException extends ApiException {

        InvalidSignatureException() {
            super(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_SIGNATURE",
                    "Invalid signature",
                    "The webhook signature is missing, invalid or expired.");
        }
    }
}
