package com.technovald.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones de la API de TechNovaLD.
 *
 * Traduce cualquier excepción lanzada durante el procesamiento de una
 * petición al cuerpo de error uniforme de la API ({@link ErrorResponse}).
 *
 * <p>Traducciones:</p>
 * <ul>
 *   <li>{@link ResourceNotFoundException} → 404</li>
 *   <li>{@link BusinessRuleException} → 400</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 (validación de DTOs)</li>
 *   <li>{@link ConstraintViolationException} → 400 (validación de parámetros sueltos)</li>
 *   <li>{@link AuthenticationException} → 401 (credenciales incorrectas)</li>
 *   <li>{@link AccountLockedException} → 423 (cuenta bloqueada temporalmente)</li>
 *   <li>{@link HttpMessageNotReadableException} → 400 (JSON ilegible)</li>
 *   <li>{@link PropertyReferenceException} → 400 (parámetro de ordenación inexistente)</li>
 *   <li>{@link DataIntegrityViolationException} → 409 (choque con una restricción UNIQUE/FK)</li>
 *   <li>{@link NoResourceFoundException} → 404 (ruta inexistente)</li>
 *   <li>Cualquier otra → 500</li>
 * </ul>
 *
 * <p><b>Los 401 de la cadena de filtros no pasan por aquí</b>: cuando falta
 * el token o no es válido, Spring Security resuelve la petición antes de
 * que llegue al {@code DispatcherServlet}. Esos 401 se documentan en la
 * sección de autenticación del README.</p>
 */
@RestControllerAdvice
@Slf4j  // Añadido para trazabilidad en consola
public class GlobalExceptionHandler {

    /**
     * Traduce {@link ResourceNotFoundException} a HTTP 404.
     *
     * @param e       la excepción lanzada por el servicio.
     * @param request la petición que provocó el error.
     * @return la respuesta con el cuerpo de error uniforme.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e,
                                                                HttpServletRequest request) {

        log.warn("Recurso no encontrado (404) en {}: {}", request.getRequestURI(), e.getMessage());

        return build(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    /**
     * Traduce {@link BusinessRuleException} a HTTP 400.
     *
     * @param e       la excepción lanzada por el servicio.
     * @param request la petición que provocó el error.
     * @return la respuesta con el cuerpo de error uniforme.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException e,
                                                            HttpServletRequest request) {

        log.warn("Regla de negocio infringida (400) en {}: {}", request.getRequestURI(), e.getMessage());

        return build(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    /**
     * Traduce {@link DataIntegrityViolationException} a HTTP 409 con mensaje genérico.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e,
                                                             HttpServletRequest request) {

        log.warn("Conflicto de integridad (409) en {}: {}", request.getRequestURI(),
                e.getMostSpecificCause().getMessage());

        return build(HttpStatus.CONFLICT, "Ya existe un registro con esos datos", request);
    }

    /**
     * Traduce {@link ConstraintViolationException} a HTTP 400.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e,
                                                                   HttpServletRequest request) {

        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validación de parámetros fallida (400) en {}: {}", request.getRequestURI(), message);

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Traduce {@link MethodArgumentNotValidException} a HTTP 400.
     *
     * <p>
     * Se lanza cuando un DTO anotado con {@code @Valid} incumple alguna
     * restricción de Jakarta Bean Validation ({@code @NotBlank},
     * {@code @Email}, {@code @Size}, {@code @Pattern}...). Se recogen
     * los errores de todos los campos y se unen en un solo mensaje con
     * el formato {@code campo: motivo}.
     * </p>
     *
     * @param e       la excepción con el resultado de la validación.
     * @param request la petición que provocó el error.
     * @return la respuesta con el cuerpo de error uniforme.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validación fallida (400) en {}: {}", request.getRequestURI(), message);

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Traduce las excepciones de autenticación de Spring Security a
     * HTTP 401.
     *
     * <p>
     * Cubre {@code BadCredentialsException}, que es la que lanza el
     * {@code AuthenticationManager} cuando el usuario o la contraseña
     * no coinciden con los guardados. El mensaje que se devuelve es
     * siempre el mismo, sin distinguir si lo que falla es el usuario o
     * la contraseña: dar pistas distintas permitiría averiguar qué
     * usuarios existen.
     * </p>
     *
     * @param e       la excepción de autenticación.
     * @param request la petición que provocó el error.
     * @return la respuesta con el cuerpo de error uniforme.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e,
                                                              HttpServletRequest request) {

        log.warn("Autenticación fallida (401) en {}: {}", request.getRequestURI(), e.getMessage());

        return build(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", request);
    }

    /**
     * Traduce {@link AccountLockedException} a HTTP 423 con {@code Retry-After}.
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException e,
                                                             HttpServletRequest request) {

        LocalDateTime hasta = e.getBloqueadoHasta();
        long segundos = (hasta == null)
                ? 60L
                : Math.max(1L, Duration.between(LocalDateTime.now(), hasta).getSeconds());

        log.warn("Cuenta bloqueada (423) en {}: quedan {} segundos", request.getRequestURI(), segundos);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(segundos));

        return build(HttpStatus.LOCKED,
                "La cuenta está bloqueada temporalmente por intentos fallidos. "
                        + "Inténtalo de nuevo más tarde.", request, headers);
    }

    /**
     * Traduce {@link HttpMessageNotReadableException} a HTTP 400.
     *
     * <p>
     * Se lanza cuando el cuerpo de la petición no se puede leer: falta
     * por completo o trae un JSON mal formado. Sin este manejador el
     * error genérico devolvería un 500, cuando el fallo es de la
     * petición.
     * </p>
     *
     * @param e       la excepción de lectura del cuerpo.
     * @param request la petición que provocó el error.
     * @return la respuesta con el cuerpo de error uniforme.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e,
                                                                  HttpServletRequest request) {

        log.warn("Cuerpo de la petición ilegible (400) en {}: {}",
                request.getRequestURI(), e.getMostSpecificCause().getMessage());

        return build(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es un JSON válido o está vacío", request);
    }

    /**
     * Traduce {@link PropertyReferenceException} a HTTP 400.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(PropertyReferenceException e,
                                                                 HttpServletRequest request) {

        String message = "Parámetro de ordenación no válido: '" + e.getPropertyName() + "'";

        log.warn("Ordenación no válida (400) en {}: {}", request.getRequestURI(), message);

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Traduce {@link NoResourceFoundException} a HTTP 404.
     *
     * <p>
     * Spring MVC la lanza cuando ninguna ruta coincide con la petición.
     * Sin este manejador, el error genérico la convertiría en un 500
     * engañoso: la ruta no existe, no es que el servidor falle.
     * </p>
     *
     * <p>
     * La ruta del mensaje se toma de la petición y no de
     * {@code e.getResourcePath()}, que para la raíz ({@code /}) viene
     * vacío.
     * </p>
     *
     * @param e       la excepción con la ruta que no se encontró.
     * @param request la petición que provocó el error.
     * @return la respuesta con el cuerpo de error uniforme.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e,
                                                               HttpServletRequest request) {

        String message = "El endpoint solicitado no existe: " + request.getRequestURI();

        log.warn("Ruta inexistente (404) en {}: {}", request.getRequestURI(), e.getMessage());

        return build(HttpStatus.NOT_FOUND, message, request);
    }

    /**
     * Fallo externo de Icecat: no es culpa del cliente ni un 500 propio.
     * Se devuelve 503 con el mensaje de la excepción.
     */
    @ExceptionHandler(IcecatApiException.class)
    public ResponseEntity<ErrorResponse> handleIcecat(IcecatApiException e,
                                                      HttpServletRequest request) {

        log.warn("Fallo al consultar Icecat (503) en {}: {}", request.getRequestURI(), e.getMessage());

        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request);
    }

    /**
     * Último recurso: captura cualquier excepción no traducida antes.
     *
     * <p>
     * Las excepciones propias de Spring MVC (método HTTP no permitido,
     * tipo de contenido no soportado, cuerpo de la petición ilegible,
     * parámetro obligatorio ausente...) ya implementan
     * {@link org.springframework.web.ErrorResponse} y traen su propio
     * código de estado. Se respeta ese código para no convertir en 500
     * un error que es del cliente; el cuerpo, en cambio, sí es el
     * uniforme de la API.
     * </p>
     *
     * <p>
     * Todo lo demás es un fallo real del servidor: se registra la traza
     * completa en el log y al cliente solo le llega un mensaje
     * genérico, sin detalles internos de la implementación.
     * </p>
     *
     * @param e       la excepción no traducida.
     * @param request la petición que provocó el error.
     * @return la respuesta con el cuerpo de error uniforme.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e,
                                                       HttpServletRequest request) {

        /*
         * El nombre va cualificado porque el record ErrorResponse anidado en
         * esta clase oculta el import de la interfaz de Spring.
         */
        if (e instanceof org.springframework.web.ErrorResponse springError
                && springError.getStatusCode() instanceof HttpStatus status) {

            log.warn("{} ({}) en {}: {}", e.getClass().getSimpleName(), status.value(),
                    request.getRequestURI(), e.getMessage());

            return build(status, e.getMessage(), request);
        }

        log.error("Error interno del servidor (500) en {}", request.getRequestURI(), e);

        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
    }

    /**
     * Construye la respuesta de error con el formato uniforme.
     *
     * @param status  código de estado HTTP de la respuesta.
     * @param message mensaje descriptivo del error.
     * @param request petición que provocó el error, de donde sale la ruta.
     * @return la respuesta lista para devolver.
     */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request) {

        return build(status, message, request, HttpHeaders.EMPTY);
    }

    /**
     * Construye la respuesta de error con el formato uniforme y cabeceras extra.
     */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request, HttpHeaders headers) {

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).headers(headers).body(body);
    }

    /**
     * Cuerpo de error uniforme de la API de TechNovaLD.
     *
     * <p>
     * Es un record de Java, así que es inmutable y sus accesores se
     * generan solos. Al estar anidado aquí no necesita fichero propio.
     * </p>
     *
     * <pre>
     * {
     *     "timestamp": "2026-10-10T13:36:49.387",
     *     "status": 404,
     *     "error": "Not Found",
     *     "message": "Empleado no encontrado con id: 5",
     *     "path": "/api/empleados/5"
     * }
     * </pre>
     *
     * @param timestamp momento en el que se produjo el error.
     * @param status    código de estado HTTP (400, 401, 404, 405, 500...).
     * @param error     frase estándar del código ("Bad Request", "Not Found"...).
     * @param message   explicación del error para el cliente.
     * @param path      ruta de la petición que falló.
     */
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path
    ) {}
}