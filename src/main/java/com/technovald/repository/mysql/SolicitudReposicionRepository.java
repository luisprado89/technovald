package com.technovald.repository.mysql;

import com.technovald.entity.SolicitudReposicion;
import com.technovald.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para {@link SolicitudReposicion}.
 *
 * <p>
 * Además de las consultas de lectura, expone la transición atómica de estado
 * que usa la recepción de la mercancía.
 * </p>
 */
@Repository
public interface SolicitudReposicionRepository extends JpaRepository<SolicitudReposicion, Long> {

    /** Busca una solicitud activa por su id. */
    Optional<SolicitudReposicion> findByIdAndActivoTrue(Long id);

    /** Lista todas las solicitudes activas. */
    List<SolicitudReposicion> findAllByActivoTrue();

    /** Lista las solicitudes activas con un estado concreto. */
    List<SolicitudReposicion> findAllByEstadoAndActivoTrue(EstadoSolicitud estado);

    /**
     * Marca la solicitud como recibida en una sola sentencia.
     *
     * <p>
     * La condición {@code estado = :estadoEsperado} y el cambio de estado
     * ocurren en el mismo {@code UPDATE}, así que dos peticiones simultáneas
     * no pueden sumar el stock dos veces: solo la primera afecta a una fila.
     * </p>
     *
     * @param id             id de la solicitud.
     * @param nuevoEstado    estado destino ({@code RECIBIDO}).
     * @param estadoEsperado estado que debe tener para aplicar el cambio
     *                       ({@code PENDIENTE}).
     * @return 1 si la transición se aplicó; 0 si ya estaba recibida, no existe
     *         o está inactiva.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SolicitudReposicion s
            set s.estado = :nuevoEstado
            where s.id = :id and s.estado = :estadoEsperado and s.activo = true
            """)
    int marcarComoRecibida(@Param("id") Long id,
                           @Param("nuevoEstado") EstadoSolicitud nuevoEstado,
                           @Param("estadoEsperado") EstadoSolicitud estadoEsperado);
}