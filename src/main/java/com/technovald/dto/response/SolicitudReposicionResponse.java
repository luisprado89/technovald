package com.technovald.dto.response;

import com.technovald.enums.EstadoSolicitud;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Datos de una solicitud de reposición.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SolicitudReposicionResponse {

    /** Id de la solicitud. */
    private Long id;

    /** Fecha de creación. */
    private LocalDateTime fechaSolicitud;

    /** Unidades solicitadas. */
    private Integer cantidad;

    /** Estado actual: PENDIENTE o RECIBIDO. */
    private EstadoSolicitud estado;

    /** Nombre corto del producto. */
    private String nombreProducto;

    /** Nombre de la empresa proveedora. */
    private String nombreProveedor;

    /** Nombre y apellidos del empleado que la creó. */
    private String nombreEmpleado;
}