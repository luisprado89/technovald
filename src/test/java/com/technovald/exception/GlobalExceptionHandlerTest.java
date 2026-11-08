package com.technovald.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/pedidos");
    }

    @Test
    void traduceReglaDeNegocioA400() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleBusinessRule(new BusinessRuleException("Stock insuficiente"), request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Stock insuficiente", response.getBody().message());
        assertEquals("/api/pedidos", response.getBody().path());
    }

    @Test
    void traduceRecursoNoEncontradoA404() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleResourceNotFound(new ResourceNotFoundException("Pedido no encontrado"), request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Pedido no encontrado", response.getBody().message());
    }

    @Test
    void traduceCredencialesInvalidasA401() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleAuthentication(new BadCredentialsException("bad credentials"), request);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Credenciales inválidas", response.getBody().message());
    }

    @Test
    void ocultaElDetalleDeUnErrorInterno() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleGeneric(new IllegalStateException("detalle interno"), request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Error interno del servidor", response.getBody().message());
    }
}
