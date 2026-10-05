package com.technovald.entity;

import com.technovald.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa una solicitud de reposición de stock en
 * TechNovaLD.
 *
 * <p>
 * Una solicitud de reposición se crea cuando un trabajador detecta que
 * el stock de un {@link ProductoInventario} ha bajado del mínimo
 * ({@code stock < stockMinimo}) y solicita al {@link Proveedor}
 * correspondiente que envíe más unidades.
 * </p>
 *
 * <p>
 * El ciclo de vida de una solicitud es simple y lineal, gobernado por
 * la enumeración {@link EstadoSolicitud}:
 * </p>
 *
 * <pre>
 * PENDIENTE → RECIBIDO
 * </pre>
 *
 * <p>
 * Cuando la solicitud pasa a {@code RECIBIDO}, el servicio debe
 * incrementar el stock del {@link ProductoInventario} asociado en la
 * cantidad indicada.
 * </p>
 *
 * <p>
 * El campo {@code fechaSolicitud} se establece automáticamente mediante
 * {@code @PrePersist} al momento de persistir la entidad.
 * </p>
 *
 * <p>
 * SolicitudReposicion no declara {@code CascadeType} en ninguna de sus
 * relaciones. El producto, el proveedor y el empleado mantienen su
 * propio ciclo de vida.
 * </p>

 * <h3>Índices definidos</h3>
 * <ul>
 *   <li>{@code @Index(name = "idx_solicitud_activo", columnList = "activo")}:
 *       índice simple sobre {@code activo}. Cubre consultas que filtran
 *       solo por estado lógico (listar todas las solicitudes activas).</li>
 *   <li>{@code @Index(name = "idx_solicitud_estado_activo", columnList = "estado, activo")}:
 *       índice compuesto que optimiza búsquedas por estado del ciclo de vida
 *       y activo simultáneamente ({@code WHERE estado = ? AND activo = true}),
 *       por ejemplo listar todas las solicitudes {@code PENDIENTE}.</li>
 *   <li>{@code @Index(name = "idx_solicitud_producto_activo", columnList = "producto_id, activo")}:
 *       índice compuesto que optimiza búsquedas por producto y activo
 *       simultáneamente ({@code WHERE producto_id = ? AND activo = true}),
 *       por ejemplo el historial de reposiciones de un producto concreto.</li>
 * </ul>
 *
 * <p><b>¿Por qué tres índices?</b> Cada uno responde a un patrón de consulta
 * real. El índice simple es necesario porque los compuestos no sirven para
 * filtrar <b>solo</b> por {@code activo}. En los compuestos el orden importa:
 * primero la columna más selectiva ({@code estado} o {@code producto_id}) y
 * después {@code activo}, que solo tiene dos valores. Todos incluyen
 * {@code activo} porque el soft delete es transversal al sistema.</p>
 *
 * <p><b>Nota:</b> Los índices aceleran las lecturas ({@code SELECT}) a cambio
 * de un leve costo en escrituras ({@code INSERT}/{@code UPDATE}/{@code DELETE})
 * y espacio en disco. Por eso se limitan a columnas realmente usadas en
 * {@code WHERE}, {@code JOIN} u {@code ORDER BY}, evitando índices
 * redundantes.</p>
 *
 * @see jakarta.persistence.Index
 * @see ProductoInventario
 * @see Proveedor
 * @see Empleado
 * @see EstadoSolicitud
 */
@Entity
@Table(name = "solicitud_reposicion", indexes = {
        @Index(name = "idx_solicitud_activo", columnList = "activo"),
        @Index(name = "idx_solicitud_estado_activo", columnList = "estado, activo"),
        @Index(name = "idx_solicitud_producto_activo", columnList = "producto_id, activo")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SolicitudReposicion {

    /**
     * Identificador único de la solicitud de reposición.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Fecha en la que se creó la solicitud de reposición.
     *
     * <p>
     * Se establece automáticamente mediante {@code @PrePersist} al
     * momento de persistir la entidad. El flag {@code updatable = false}
     * garantiza que no pueda modificarse en futuras actualizaciones.
     * </p>
     */
    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private LocalDateTime fechaSolicitud;

    /**
     * Cantidad de unidades solicitadas al proveedor.
     *
     * <p>
     * Cuando la solicitud pase a estado {@code RECIBIDO}, esta cantidad
     * se sumará al stock actual del {@link ProductoInventario} asociado.
     * </p>
     */
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    /**
     * Estado actual de la solicitud dentro del ciclo de vida lineal.
     *
     * <p>
     * Se inicializa como {@code PENDIENTE} de forma predeterminada
     * mediante {@code @Builder.Default}. El servicio de solicitudes es
     * el responsable de validar la transición a {@code RECIBIDO}.
     * </p>
     *
     * <p>
     * Se persiste como texto literal en MySQL mediante
     * {@code @Enumerated(EnumType.STRING)}. Se define explícitamente como
     * {@code VARCHAR(50)} usando {@code columnDefinition} para evitar que
     * Hibernate 6+ intente alterar o recrear la columna como un tipo
     * {@code ENUM} nativo en cada arranque, garantizando mayor portabilidad
     * y permitiendo consultar solicitudes por estado de forma legible desde SQL.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)", nullable = false)
    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    /**
     * Producto cuyo stock se desea reponer.
     *
     * <p>
     * SolicitudReposicion es el dueño de la relación: el FK
     * {@code producto_id} vive en la tabla
     * {@code solicitud_reposicion}. Toda solicitud debe tener un
     * producto asociado.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar los datos del producto
     * cuando solo se necesita información de la propia solicitud.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoInventario producto;

    /**
     * Proveedor al que se le solicita la reposición.
     *
     * <p>
     * SolicitudReposicion es el dueño de la relación: el FK
     * {@code proveedor_id} vive en la tabla
     * {@code solicitud_reposicion}. Toda solicitud debe tener un
     * proveedor asociado.
     * </p>
     *
     * <p>
     * Normalmente el proveedor coincide con el proveedor habitual del
     * producto, pero se modela como relación independiente para
     * permitir solicitar reposición a un proveedor distinto en casos
     * excepcionales.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar los datos del
     * proveedor cuando solo se necesita información de la propia
     * solicitud.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    /**
     * Empleado que gestionó o creó la solicitud de reposición.
     *
     * <p>
     * SolicitudReposicion es el dueño de la relación: el FK
     * {@code empleado_id} vive en la tabla
     * {@code solicitud_reposicion}. Toda solicitud debe tener un
     * empleado asociado.
     * </p>
     *
     * <p>
     * Esta relación sustituye cualquier FK directo a {@link Usuario}
     * que pudiera existir en versiones anteriores del proyecto.
     * La asociación con {@link Empleado} garantiza que la solicitud está
     * vinculada al perfil laboral, no a las credenciales de
     * autenticación.
     * </p>
     *
     * <p>
     * Solo los empleados con rol {@code TRABAJADOR} o {@code ADMIN}
     * pueden crear solicitudes de reposición. El servicio valida esta
     * regla de negocio.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar los datos del empleado
     * cuando solo se necesita información de la propia solicitud.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    /**
     * Indica si la solicitud se encuentra activa en el sistema.
     *
     * <p>
     * Se utiliza para el borrado lógico (soft delete). Las solicitudes
     * desactivadas permanecen en la base de datos para auditoría pero
     * no aparecen en las consultas estándar.
     * </p>
     *
     * <p>
     * {@code @Builder.Default} es vital aquí: sin esta anotación,
     * Lombok Builder dejaría el campo en {@code null} en lugar de
     * {@code true}.
     * </p>
     */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /**
     * Callback de JPA que se ejecuta automáticamente antes de persistir
     * la entidad por primera vez (INSERT).
     *
     * <p>
     * Establece la fecha de la solicitud al momento actual. De esta
     * forma no es necesario preocuparse por setear este campo en la
     * capa de servicio.
     * </p>
     */
    @PrePersist
    protected void onCreate() {
        this.fechaSolicitud = LocalDateTime.now();
    }
}