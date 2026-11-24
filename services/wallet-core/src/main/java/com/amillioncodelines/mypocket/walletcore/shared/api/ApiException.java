package com.amillioncodelines.mypocket.walletcore.shared.api;

import org.springframework.http.HttpStatus;

/** Business error that maps to a Problem Details response with a stable {@code code}. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;

    protected ApiException(HttpStatus status, String code, String title, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }
}
