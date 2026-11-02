package com.technovald.controller;

import com.technovald.dto.request.ProveedorRequest;
import com.technovald.dto.response.ProveedorResponse;
import com.technovald.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la gestión de proveedores.
 * La lectura está abierta a ADMIN y TRABAJADOR; la escritura solo a ADMIN.
 */
@RestController
@RequestMapping("/api/admin/proveedores")
@RequiredArgsConstructor
public class AdminProveedorController {

    private final ProveedorService proveedorService;

    /**
     * Devuelve todos los proveedores, incluyendo los inactivos.
     *
     * @return lista completa de proveedores.
     */
    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listarProveedores() {
        return ResponseEntity.ok(proveedorService.listarProveedores());
    }

    /**
     * Devuelve solo los proveedores activos.
     * Se usará para alimentar los desplegables en los formularios de pedidos y reposición.
     *
     * @return lista de proveedores activos.
     */
    @GetMapping("/active")
    public ResponseEntity<List<ProveedorResponse>> listarProveedoresActivos() {
        return ResponseEntity.ok(proveedorService.listarProveedoresActivos());
    }

    /**
     * Crea un nuevo proveedor.
     *
     * <p>Responde {@code 201 Created} sin cabecera {@code Location} porque
     * actualmente no existe un endpoint de consulta individual por ID.</p>
     *
     * @param proveedorRequest datos del nuevo proveedor.
     * @return el proveedor creado.
     */
    @PostMapping
    public ResponseEntity<ProveedorResponse> crearProveedor(@Valid @RequestBody ProveedorRequest proveedorRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proveedorService.crearProveedor(proveedorRequest));
    }

    /**
     * Actualiza los datos de un proveedor existente.
     *
     * @param id identificador del proveedor.
     * @param proveedorRequest nuevos datos del proveedor.
     * @return el proveedor actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponse> actualizarProveedor(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorRequest proveedorRequest) {

        return ResponseEntity.ok(proveedorService.actualizarProveedor(id, proveedorRequest));
    }

    /**
     * Realiza la baja lógica de un proveedor.
     *
     * @param id identificador del proveedor a desactivar.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarProveedor(@PathVariable Long id) {
        proveedorService.desactivarProveedor(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva un proveedor previamente dado de baja.
     *
     * @param id identificador del proveedor a reactivar.
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivarProveedor(@PathVariable Long id) {
        proveedorService.reactivarProveedor(id);
        return ResponseEntity.noContent().build();
    }
}