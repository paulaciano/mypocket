package com.amillioncodelines.mypocket.walletcore.user.domain;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** U2: not blank, 8 to 64 characters and at most 72 bytes in UTF-8 (the BCrypt limit). */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AcceptablePassword.Validator.class)
public @interface AcceptablePassword {

    String message() default "must not be blank and have between 8 and 64 characters, at most 72 bytes in UTF-8";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<AcceptablePassword, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return User.isAcceptablePassword(value);
        }
    }
}
