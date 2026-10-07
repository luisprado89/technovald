package com.technovald.controller;


import com.technovald.dto.request.LoginRequest;
import com.technovald.dto.response.AuthResponse;
import com.technovald.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}
