package com.technovald.service;


import com.technovald.dto.request.LoginRequest;
import com.technovald.dto.response.AuthResponse;

/**
 * Interfaz que define las operaciones de autenticación pura de TechNovaLD.
 *
 * <p>
 * Se diseñó siguiendo el principio de responsabilidad única: esta interfaz
 * NO gestiona la creación de perfiles (clientes o empleados), ya que esa
 * lógica reside en {@code ClienteService} y {@code EmpleadoService}.
 * Aquí solo se trata la validación de credenciales (login) y la gestión
 * de recuperación y cambio de contraseñas.
 * </p>
 */
public interface AuthService {

    /**
     * Autentica a un usuario mediante su username y contraseña.
     *
     * <p>
     * Delega la validación a Spring Security y, si es exitosa, genera
     * un token JWT para autorizar las peticiones posteriores.
     * </p>
     *
     * @param loginRequest DTO con las credenciales de acceso.
     * @return DTO con el token JWT, el username, el rol y la fecha de expiración.
     */
    AuthResponse login(LoginRequest loginRequest);
}
