package com.technovald.controller;

import com.technovald.dto.request.EmpleadoRegistroRequest;
import com.technovald.dto.response.EmpleadoRegistroResponse;
import com.technovald.dto.response.EmpleadoResponse;
import com.technovald.service.EmpleadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST de gestión de empleados.
 *
 * <p>
 * Expone el alta y la consulta del personal. Las tres rutas cuelgan de
 * {@code /api/admin/empleados}, que la cadena de seguridad de
 * {@code com.technovald.config.SecurityConfig} reserva al rol {@code ADMIN}:
 * el controlador no repite la comprobación, porque la regla vive en un único
 * sitio y así ningún endpoint de administración puede quedarse sin proteger.
 * </p>
 *
 * <p>
 * La creación afecta a tres tablas ({@code persona}, {@code usuario} y
 * {@code empleado}), pero el controlador no lo sabe: delega en el servicio y se
 * limita a traducir el resultado a HTTP.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/empleados")
@RequiredArgsConstructor
public class AdminEmpleadoController {

    /**
     * Servicio con la lógica de alta y de consulta de empleados.
     */
    private final EmpleadoService empleadoService;

    /**
     * Da de alta un empleado y devuelve sus credenciales iniciales.
     *
     * <p>
     * Responde {@code 201} con la cabecera {@code Location} apuntando a
     * {@code GET /api/admin/empleados/{id}}, que existe: la respuesta no apunta
     * a una ruta que todavía no se puede consultar.
     * </p>
     *
     * @param empleadoRegistroRequest datos personales y rol del empleado.
     * @return las credenciales generadas y el rol del empleado creado.
     */
    @PostMapping
    public ResponseEntity<EmpleadoRegistroResponse> createEmpleado(
            @Valid @RequestBody EmpleadoRegistroRequest empleadoRegistroRequest) {

        EmpleadoRegistroResponse response =
                empleadoService.createEmpleado(empleadoRegistroRequest);

        return ResponseEntity
                .created(URI.create("/api/admin/empleados/" + response.getId()))
                .body(response);
    }

    /**
     * Devuelve el directorio completo de empleados, ordenado por id.
     *
     * @return la lista de empleados.
     */
    @GetMapping
    public ResponseEntity<List<EmpleadoResponse>> getAllEmpleados() {
        return ResponseEntity.ok(empleadoService.getAllEmpleados());
    }

    /**
     * Devuelve un empleado activo por su identificador.
     *
     * @param id identificador del perfil de empleado.
     * @return los datos del empleado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmpleadoResponse> getEmpleadoById(@PathVariable Long id) {
        return ResponseEntity.ok(empleadoService.getEmpleadoById(id));
    }
}