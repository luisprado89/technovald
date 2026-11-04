package com.technovald.service;

import com.technovald.dto.request.SolicitudReposicionRequest;
import com.technovald.dto.response.SolicitudReposicionResponse;
import com.technovald.enums.EstadoSolicitud;

import java.util.List;

/** Lógica de las solicitudes de reposición de stock. */
public interface SolicitudReposicionService {

    /**
     * Crea una solicitud para el empleado autenticado.
     *
     * @param solicitudReposicionRequest  producto, proveedor y cantidad.
     * @param username usuario autenticado.
     * @return la solicitud creada.
     */
    SolicitudReposicionResponse crearSolicitud(SolicitudReposicionRequest solicitudReposicionRequest, String username);

    /**
     * Marca una solicitud como recibida y repone el stock del producto.
     *
     * @param id id de la solicitud.
     * @return la solicitud actualizada.
     */
    SolicitudReposicionResponse recibirSolicitud(Long id);

    /**
     * Lista las solicitudes activas, opcionalmente filtradas por estado.
     *
     * @param estado estado a filtrar, o {@code null} para todas.
     * @return las solicitudes.
     */
    List<SolicitudReposicionResponse> getAllSolicitudes(EstadoSolicitud estado);
}