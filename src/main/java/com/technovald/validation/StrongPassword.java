package com.technovald.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Política mínima de contraseñas: longitud, clases de caracteres y lista de comunes.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "La contraseña no cumple la política de seguridad";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
