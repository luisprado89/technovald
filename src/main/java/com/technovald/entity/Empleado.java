package com.technovald.entity;


import com.technovald.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa el perfil de empleado en TechNovaLD.
 *
 * <p>
 * Empleado es un perfil de dominio que "envuelve" a una {@link Persona}
 * para añadirle la dimensión laboral. Un empleado puede gestionar
 * compras de clientes, responder comentarios y crear solicitudes de
 * reposición de stock.
 * </p>
 *
 * <p>
 * La estructura del modelo de identidad de TechNovaLD separa los datos
 * personales (Persona), las credenciales (Usuario) y los perfiles de
 * dominio (Cliente, Empleado) en tablas independientes. Empleado es el
 * dueño de la relación con Persona: el FK {@code persona_id} vive en
 * la tabla {@code empleado}.
 * </p>
 *
 * <p>
 * El campo {@code rol} es una desnormalización intencional que
 * duplica el rol del {@link Usuario} asociado. Mientras que
 * {@code Usuario.rol} es la fuente canónica para RBAC (leída por el
 * filtro JWT), {@code Empleado.rol} permite consultar el sub-rol del
 * empleado ({@code ADMIN} o {@code TRABAJADOR}) sin necesidad de
 * realizar un JOIN con la tabla {@code usuario}.
 * </p>
 *
 * <p>
 * Los valores válidos para {@code Empleado.rol} son
 * {@code RolUsuario.ADMIN} y {@code RolUsuario.TRABAJADOR}. El valor
 * {@code RolUsuario.CLIENTE} nunca se asigna a un empleado.
 * </p>
 *
 * <p>
 * Empleado no declara {@code CascadeType} en la relación con Persona ni
 * con SolicitudReposicion. La filosofía es que cada entidad mantiene su
 * propio ciclo de vida y no debe eliminarse en cascada.
 * </p>
 *
 * <p><b>Índice:</b> definido con {@code @Index} usando {@code name = "idx_empleado_activo"}
 * y {@code columnList = "activo"}, para optimizar búsquedas frecuentes por estado
 * (empleados activos/inactivos).</p>
 *
 * <p><b>Nota:</b> Los índices aceleran las lecturas ({@code SELECT}) a cambio de
 * un leve costo en escrituras ({@code INSERT}/{@code UPDATE}/{@code DELETE}) y
 * espacio en disco. Solo deben aplicarse sobre columnas usadas frecuentemente
 * en {@code WHERE}, {@code JOIN} u {@code ORDER BY}.</p>
 *
 * @see jakarta.persistence.Index
 */
@Entity
@Table(name = "empleado", indexes = {
        @Index(name = "idx_empleado_activo", columnList = "activo")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Empleado {

    /**
     * Identificador único del empleado.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Persona asociada al empleado.
     *
     * <p>
     * Empleado es el dueño de la relación: el FK {@code persona_id}
     * vive en la tabla {@code empleado}. Una persona puede tener como
     * máximo un perfil de empleado.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar los datos personales
     * cuando solo se necesita el identificador del empleado.
     * </p>
     *
     * <p>
     * {@code nullable = false} y {@code unique = true}: todo empleado
     * debe tener una persona, y una persona solo puede tener un
     * perfil de empleado.
     * </p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    private Persona persona;

    /**
     * Rol del empleado dentro del sistema.
     *
     * <p>
     * Es una desnormalización intencional del campo
     * {@link Usuario#getRol()}. Permite consultar el sub-rol del
     * empleado sin realizar un JOIN con la tabla {@code usuario}.
     * </p>
     *
     * <p>
     * Los valores válidos son:
     * </p>
     * <ul>
     *     <li>{@code ADMIN}: administrador con acceso total al sistema.</li>
     *     <li>{@code TRABAJADOR}: trabajador operativo que gestiona
     *     compras, responde comentarios y crea solicitudes de
     *     reposición.</li>
     * </ul>
     *
     * <p>
     * El valor {@code CLIENTE} nunca se asigna a un empleado. El
     * servicio de creación valida esta regla de negocio.
     * </p>
     *
     * <p>
     * Se persiste como texto literal en MySQL mediante
     * {@code @Enumerated(EnumType.STRING)}. Se define explícitamente como
     * {@code VARCHAR(50)} usando {@code columnDefinition} para evitar que
     * Hibernate 6+ intente alterar o recrear la columna como un tipo
     * {@code ENUM} nativo en cada arranque, garantizando mayor portabilidad
     * y permitiendo consultar empleados por rol de forma legible desde SQL.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)", nullable = false)
    private RolUsuario rol;

    /**
     * Lista de solicitudes de reposición gestionadas por el empleado.
     *
     * <p>
     * Relación inversa: el FK {@code empleado_id} vive en la tabla
     * {@code solicitud_reposicion}. Esta lista se carga de forma
     * perezosa ({@code FetchType.LAZY}) para evitar recuperar todas
     * las solicitudes del empleado cuando solo se necesita información
     * del propio perfil.
     * </p>
     *
     * <p>
     * Las solicitudes mantienen su propio ciclo de vida y no se
     * eliminan al desactivar el empleado. Esto garantiza la
     * persistencia del historial de gestiones para auditoría.
     * </p>
     *
     * <p>
     * {@code @Builder.Default} inicializa la lista como
     * {@code new ArrayList<>()}. Sin esta anotación, Lombok Builder
     * dejaría el campo en {@code null}.
     * </p>
     */
    @OneToMany(mappedBy = "empleado", fetch = FetchType.LAZY)
    @Builder.Default
    private List<SolicitudReposicion> solicitudesReposicion = new ArrayList<>();

    /**
     * Indica si el perfil de empleado se encuentra activo.
     *
     * <p>
     * Se utiliza para el borrado lógico (soft delete). Al desactivar
     * un empleado, el servicio también desactiva la {@link Persona}
     * y el {@link Usuario} asociados, impidiendo que el empleado pueda
     * autenticarse.
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
}
