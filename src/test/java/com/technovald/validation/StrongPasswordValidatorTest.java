package com.technovald.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void rechazaMenosDeDiezCaracteres() {
        assertFalse(validator.isValid("Ab1!", null));
    }

    @Test
    void rechazaMenosDeTresClases() {
        assertFalse(validator.isValid("abcdefghij", null));
        assertFalse(validator.isValid("abcdefgh12", null));
    }

    @Test
    void rechazaUnaContrasenaComun() {
        assertFalse(validator.isValid("Password123", null));
    }

    @Test
    void rechazaMasDeSetentaYDosBytes() {
        assertFalse(validator.isValid("A1!" + "a".repeat(70), null));
    }

    @Test
    void aceptaUnaContrasenaFuerte() {
        assertTrue(validator.isValid("Xk9#mP2!vL", null));
    }

    @Test
    void delegaLaNulidadEnOtrasAnotaciones() {
        assertTrue(validator.isValid(null, null));
    }
}
