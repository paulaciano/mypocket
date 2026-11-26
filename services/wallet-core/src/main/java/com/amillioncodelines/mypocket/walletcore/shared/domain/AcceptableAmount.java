package com.amillioncodelines.mypocket.walletcore.shared.domain;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

/** M1, M2 on a JSON amount string: plain digits with an optional dot, then {@link Money#isAcceptable}. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AcceptableAmount.Validator.class)
public @interface AcceptableAmount {

    String message() default "must be a positive amount with at most 2 decimals and 13 integer digits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<AcceptableAmount, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            return value.matches("[0-9]{1,20}(\\.[0-9]{1,20})?") && Money.isAcceptable(new BigDecimal(value));
        }
    }
}
