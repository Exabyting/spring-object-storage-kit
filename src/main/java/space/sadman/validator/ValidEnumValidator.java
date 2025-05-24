package space.sadman.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import space.sadman.annotation.ValidEnum;

public class ValidEnumValidator implements ConstraintValidator<ValidEnum, Object> {
    private Class<? extends Enum<?>> enumClass;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        if (value instanceof String) {
            for (Enum<?> e : enumClass.getEnumConstants()) {
                if (e.name().equals(value)) return true;
            }
            return false;
        } else return enumClass.isInstance(value);
    }
}
