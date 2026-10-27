package com.technovald.controller;

import com.technovald.dto.request.EmpleadoRegistroRequest;
import com.technovald.dto.request.EmpleadoUpdateRequest;
import com.technovald.dto.response.EmpleadoRegistroResponse;
import com.technovald.dto.response.EmpleadoResponse;
import com.technovald.service.EmpleadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Actualiza los datos editables de un empleado (incluido su rol).
     *
     * <p>El DNI y el username no se pueden modificar, ya que identifican a la
     * persona y a la cuenta de acceso de forma unívoca.</p>
     *
     * @param id identificador del empleado.
     * @param empleadoUpdateRequest nuevos datos a aplicar.
     * @return los datos actualizados del empleado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EmpleadoResponse> updateEmpleado(
            @PathVariable Long id,
            @Valid @RequestBody EmpleadoUpdateRequest empleadoUpdateRequest) {

        return ResponseEntity.ok(empleadoService.updateEmpleado(id, empleadoUpdateRequest));
    }

    /**
     * Realiza la baja lógica de un empleado.
     *
     * <p>No elimina los datos de la base de datos. Desactiva el perfil y el
     * usuario para impedir que el empleado pueda iniciar sesión.</p>
     *
     * @param id identificador del empleado a desactivar.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarEmpleado(@PathVariable Long id) {
        empleadoService.desactivarEmpleado(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva a un empleado previamente dado de baja.
     *
     * <p>Vuelve a habilitar el acceso del usuario, permitiéndole iniciar sesión
     * de nuevo si dispone de sus credenciales.</p>
     *
     * @param id identificador del empleado a reactivar.
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivarEmpleado(@PathVariable Long id) {
        empleadoService.reactivarEmpleado(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resetea la contraseña de un empleado.
     *
     * <p>Genera una contraseña temporal aleatoria, invalida los tokens anteriores
     * e informa al administrador para que se la entregue al empleado.</p>
     *
     * @param id identificador del empleado.
     * @return las nuevas credenciales generadas (username y contraseña temporal).
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<EmpleadoRegistroResponse> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(empleadoService.resetPassword(id));
    }
}