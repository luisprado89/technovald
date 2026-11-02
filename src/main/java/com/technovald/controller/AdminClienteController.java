package com.technovald.controller;

import com.technovald.dto.request.ClienteUpdateRequest;
import com.technovald.dto.response.ClienteAdminResponse;
import com.technovald.dto.response.PaginaResponse;
import com.technovald.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST de gestión de clientes para el staff (ADMIN y TRABAJADOR).
 *
 * <p>
 * Expone el listado paginado, consulta, edición y baja/reactivación de los
 * clientes. El perfil propio del cliente sigue gestionándose en
 * {@code /api/clientes/me}.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/clientes")
@RequiredArgsConstructor
public class AdminClienteController {

    private final ClienteService clienteService;

    /**
     * Devuelve el directorio paginado de clientes (activos e inactivos).
     *
     * @param pageable parámetros de paginación (página y tamaño).
     * @return página de clientes con metadatos de paginación.
     */
    @GetMapping
    public ResponseEntity<PaginaResponse<ClienteAdminResponse>> listarClientes(
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(PaginaResponse.de(clienteService.listarClientes(pageable)));
    }

    /**
     * Obtiene el detalle de un cliente por su identificador.
     *
     * @param id identificador del cliente.
     * @return los datos del cliente.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClienteAdminResponse> obtenerCliente(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerCliente(id));
    }

    /**
     * Actualiza los datos de contacto de un cliente.
     *
     * <p>Solo se puede modificar el email, el teléfono y la dirección. El nombre, apellidos y DNI son inmutables.</p>
     *
     * @param id identificador del cliente.
     * @param clienteUpdateRequest nuevos datos de contacto.
     * @return los datos del cliente actualizados.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClienteAdminResponse> actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteUpdateRequest clienteUpdateRequest) {

        return ResponseEntity.ok(clienteService.actualizarCliente(id, clienteUpdateRequest));
    }

    /**
     * Realiza la baja lógica de un cliente.
     *
     * <p>Desactiva el cliente, la persona y el usuario para impedir su inicio de sesión.</p>
     *
     * @param id identificador del cliente a desactivar.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarCliente(@PathVariable Long id) {
        clienteService.desactivarCliente(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva a un cliente previamente dado de baja.
     *
     * @param id identificador del cliente a reactivar.
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivarCliente(@PathVariable Long id) {
        clienteService.reactivarCliente(id);
        return ResponseEntity.noContent().build();
    }
}