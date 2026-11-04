package com.technovald.controller;

import com.technovald.dto.request.ComentarioRequest;
import com.technovald.dto.request.RespuestaComentarioRequest;
import com.technovald.dto.response.ComentarioPendienteResponse;
import com.technovald.dto.response.ComentarioResponse;
import com.technovald.service.ComentarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reseñas de productos. La lectura es pública; el cliente publica y el staff
 * responde. Los permisos se aplican por URL en {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/comentarios")
@RequiredArgsConstructor
public class ComentarioController {

    private final ComentarioService comentarioService;

    /**
     * Reseñas activas de un producto.
     *
     * @param productoInventarioId id del producto en MySQL.
     * @return la lista de reseñas.
     */
    @GetMapping("/producto/{productoInventarioId}")
    public ResponseEntity<List<ComentarioResponse>> listarPorProducto(
            @PathVariable Long productoInventarioId) {

        return ResponseEntity.ok(comentarioService.getComentariosByProducto(productoInventarioId));
    }

    /**
     * Publica una reseña.
     *
     * @param comentarioRequest     producto, texto y valoración.
     * @param userDetails cliente autenticado.
     * @return la reseña creada (201).
     */
    @PostMapping
    public ResponseEntity<ComentarioResponse> crearComentario(
            @Valid @RequestBody ComentarioRequest comentarioRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioService.crearComentario(comentarioRequest, userDetails.getUsername()));
    }

    /**
     * Responde oficialmente a una reseña.
     *
     * @param comentarioId UUID de la reseña.
     * @param respuestaComentarioRequest      texto de la respuesta.
     * @param userDetails  trabajador o administrador autenticado.
     * @return la reseña actualizada.
     */
    @PutMapping("/{comentarioId}/responder")
    public ResponseEntity<ComentarioResponse> responderComentario(
            @PathVariable String comentarioId,
            @Valid @RequestBody RespuestaComentarioRequest respuestaComentarioRequest,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                comentarioService.responderComentario(comentarioId, respuestaComentarioRequest, userDetails.getUsername()));
    }

    /**
     * Bandeja de reseñas sin respuesta.
     *
     * @return las reseñas pendientes con su producto.
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<ComentarioPendienteResponse>> listarComentariosPendientes() {
        return ResponseEntity.ok(comentarioService.getComentariosPendientes());
    }
}