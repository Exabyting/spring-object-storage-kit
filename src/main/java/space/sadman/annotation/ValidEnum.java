package space.sadman.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import space.sadman.validator.ValidEnumValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidEnumValidator.class )
@Target({
        ElementType.FIELD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {
    String message() default "must be a valid enum value";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    Class<? extends java.lang.Enum<?>> enumClass();
}
