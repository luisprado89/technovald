package com.technovald.controller;

import com.technovald.dto.request.CategoriaRequest;
import com.technovald.dto.response.CategoriaResponse;
import com.technovald.service.CategoriaService;
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
 * Controlador REST para la gestión de categorías.
 * Las rutas de escritura están reservadas a ADMIN; la lectura está abierta a ADMIN y TRABAJADOR.
 */
@RestController
@RequestMapping("/api/admin/categorias")
@RequiredArgsConstructor
public class AdminCategoriaController {

    private final CategoriaService categoriaService;

    /**
     * Devuelve la lista completa de categorías (activas e inactivas).
     *
     * @return lista de categorías ordenadas por nombre.
     */
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listar() {
        return ResponseEntity.ok(categoriaService.listarCategorias());
    }

    /**
     * Crea una nueva categoría.
     *
     * <p>Responde {@code 201 Created} sin cabecera {@code Location} porque no
     * existe un endpoint de consulta individual por ID.</p>
     *
     * @param request datos de la nueva categoría.
     * @return la categoría creada.
     */
    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoriaService.crearCategoria(request));
    }

    /**
     * Actualiza el nombre y la descripción de una categoría existente.
     *
     * @param id identificador de la categoría.
     * @param request nuevos datos de la categoría.
     * @return la categoría actualizada.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest request) {

        return ResponseEntity.ok(categoriaService.actualizarCategoria(id, request));
    }

    /**
     * Realiza la baja lógica de una categoría.
     *
     * @param id identificador de la categoría a desactivar.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        categoriaService.desactivarCategoria(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva una categoría previamente dada de baja.
     *
     * @param id identificador de la categoría a reactivar.
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable Long id) {
        categoriaService.reactivarCategoria(id);
        return ResponseEntity.noContent().build();
    }
}