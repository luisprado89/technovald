package com.technovald.security;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationCodeGeneratorTest {

    private final VerificationCodeGenerator generator = new VerificationCodeGenerator();

    @Test
    void generaSiempreSeisDigitos() {
        List<String> codigos = IntStream.range(0, 10_000)
                .mapToObj(i -> generator.generarCodigo())
                .toList();

        assertTrue(codigos.stream().allMatch(codigo -> codigo.matches("\\d{6}")));
    }

    @Test
    void conservaLosCerosALaIzquierda() {
        boolean hayCero = IntStream.range(0, 10_000)
                .mapToObj(i -> generator.generarCodigo())
                .anyMatch(codigo -> codigo.startsWith("0"));

        assertTrue(hayCero);
    }
}
