package space.sadman.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(name = "object-storage-kit.storage-type", havingValue = "s3")
public @interface ConditionalOnStorageType {
    /**
     * The storage type value to match against the property.
     * Defaults to "s3".
     */
    @AliasFor(annotation = ConditionalOnProperty.class, attribute = "havingValue")
    String value() default "s3";
}
