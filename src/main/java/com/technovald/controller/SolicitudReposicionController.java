package com.technovald.controller;

import com.technovald.dto.request.SolicitudReposicionRequest;
import com.technovald.dto.response.SolicitudReposicionResponse;
import com.technovald.enums.EstadoSolicitud;
import com.technovald.service.SolicitudReposicionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Solicitudes de reposición. El staff pide stock al proveedor y registra la
 * recepción; los permisos se aplican por URL en {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudReposicionController {

    private final SolicitudReposicionService solicitudService;

    /**
     * Crea una solicitud para el empleado autenticado.
     *
     * @param request     producto, proveedor y cantidad.
     * @param userDetails usuario autenticado.
     * @return la solicitud creada (201).
     */
    @PostMapping
    public ResponseEntity<SolicitudReposicionResponse> crearSolicitud(
            @Valid @RequestBody SolicitudReposicionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solicitudService.crearSolicitud(request, userDetails.getUsername()));
    }

    /**
     * Marca una solicitud como recibida y repone el stock.
     *
     * @param id id de la solicitud.
     * @return la solicitud actualizada.
     */
    @PutMapping("/{id}/recibir")
    public ResponseEntity<SolicitudReposicionResponse> recibirSolicitud(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.recibirSolicitud(id));
    }

    /**
     * Lista las solicitudes activas, con filtro opcional por estado.
     *
     * @param estado estado a filtrar, o {@code null} para todas.
     * @return las solicitudes.
     */
    @GetMapping
    public ResponseEntity<List<SolicitudReposicionResponse>> getAllSolicitudes(
            @RequestParam(required = false) EstadoSolicitud estado) {

        return ResponseEntity.ok(solicitudService.getAllSolicitudes(estado));
    }
}