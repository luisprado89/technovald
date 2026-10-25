package com.technovald.controller;


import com.technovald.dto.request.ChangePasswordRequest;
import com.technovald.dto.request.ForgotPasswordRequest;
import com.technovald.dto.request.LoginRequest;
import com.technovald.dto.response.AuthResponse;
import com.technovald.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST encargado de gestionar las operaciones de autenticación.
 *
 * <p>
 * Expone los endpoints necesarios para el inicio de sesión, la recuperación
 * de contraseñas olvidadas y el cambio de contraseña para usuarios ya
 * autenticados.
 * </p>
 *
 * <p>
 * El acceso a los endpoints de inicio de sesión y recuperación es público.
 * El cambio de contraseña requiere que el usuario esté autenticado.
 * </p>
 */

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * Servicio encargado de gestionar la lógica de autenticación.
     */
    private final AuthService authService;

    /**
     * Inicia sesión y devuelve el token.
     *
     * @param request credenciales del usuario.
     * @return el token y los datos del usuario autenticado.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Inicia el proceso de recuperación de contraseña.
     *
     * <p>Responde siempre 204 (sin cuerpo) para no revelar si el email existe
     * en el sistema. Si la cuenta está activa, recibe una contraseña temporal.</p>
     *
     * @param forgotPasswordRequest DTO con el email a recuperar.
     * @return respuesta vacía (204).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        authService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.noContent().build();
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * <p>El nombre de usuario se extrae del token JWT. Al cambiarla, se
     * invalidan los tokens anteriores.</p>
     *
     * @param userDetails           usuario autenticado extraído del JWT.
     * @param changePasswordRequest DTO con la contraseña actual y la nueva.
     * @return respuesta vacía (204).
     */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                               @Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        authService.changePassword(userDetails.getUsername(), changePasswordRequest);
        return ResponseEntity.noContent().build();
    }
}
