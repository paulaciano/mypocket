package com.amillioncodelines.mypocket.walletcore.user.domain;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * U1: at most 254 characters once normalized. Lower-casing can make an email longer (for example
 * "İ"), so the length is checked after normalization. Null is left to {@code @NotBlank}.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AcceptableEmail.Validator.class)
public @interface AcceptableEmail {

    String message() default "must have at most 254 characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<AcceptableEmail, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || User.normalizeEmail(value).length() <= User.MAX_EMAIL_LENGTH;
        }
    }
}
