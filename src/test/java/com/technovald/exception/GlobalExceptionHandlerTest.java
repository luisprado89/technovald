package com.technovald.exception;

import com.mongodb.MongoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void traduceConflictoDeIntegridadA409() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException(
                        "Duplicate entry '12345678Z' for key 'uk_persona_dni'"),
                request);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Ya existe un registro con esos datos", response.getBody().message());
        assertFalse(response.getBody().message().contains("uk_persona_dni"),
                "el nombre de la constraint no debe llegar al cliente");
    }

    @Test
    void traduceOrdenacionNoValidaA400() {
        PropertyReferenceException ex = new PropertyReferenceException(
                "inventado", TypeInformation.of(String.class), List.of());

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handlePropertyReference(ex, request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().message().contains("inventado"));
    }

    @Test
    void traduceCuentaBloqueadaA423ConRetryAfter() {
        AccountLockedException ex = new AccountLockedException(
                "Cuenta bloqueada", LocalDateTime.now().plusMinutes(15));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleAccountLocked(ex, request);

        assertEquals(423, response.getStatusCode().value());
        String retryAfter = response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        assertNotNull(retryAfter, "el 423 debe indicar cuándo reintentar");
        long segundos = Long.parseLong(retryAfter);
        assertTrue(segundos > 0 && segundos <= 15 * 60);
    }

    @Test
    void traduceClaveDuplicadaDeMongoA409() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleMongo(new MongoException(11000, "E11000 duplicate key"), request);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Ya existe un registro con esos datos", response.getBody().message());
    }

    @Test
    void traduceErrorDeMongoA500ConMensajeGenerico() {
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleMongo(new MongoException(2, "fallo interno del driver"), request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Error al procesar la solicitud en el catálogo. Inténtalo de nuevo más tarde.",
                response.getBody().message());
    }
}
