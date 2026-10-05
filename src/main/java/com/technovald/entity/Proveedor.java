package com.technovald.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad maestra que representa un proveedor en TechNovaLD.
 *
 * <p>
 * Un proveedor es una empresa externa que suministra productos al
 * almacén. Cada {@link ProductoInventario} tiene un proveedor habitual
 * asociado, y las {@link SolicitudReposicion} se dirigen a un
 * proveedor concreto.
 * </p>
 *
 * <p>
 * Proveedor no declara {@code CascadeType} en ninguna relación.
 * Cada entidad mantiene su propio ciclo de vida.
 * </p>
 * <p><b>Índice:</b> definido con {@code @Index} usando {@code name = "idx_proveedor_activo"}
 * y {@code columnList = "activo"}, para optimizar búsquedas frecuentes por estado
 * (proveedores activos/inactivos).</p>
 *
 * <p><b>Nota:</b> Los índices aceleran las lecturas ({@code SELECT}) a cambio de
 * un leve costo en escrituras ({@code INSERT}/{@code UPDATE}/{@code DELETE}) y
 * espacio en disco. Solo deben aplicarse sobre columnas usadas frecuentemente
 * en {@code WHERE}, {@code JOIN} u {@code ORDER BY}.</p>
 *
 * @see jakarta.persistence.Index
 */
@Entity
@Table(name = "proveedor", indexes = {
        @Index(name = "idx_proveedor_activo", columnList = "activo")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Proveedor {

    /**
     * Identificador único del proveedor.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de la empresa proveedora.
     *
     * <p>
     * Ej: ASUS España, Logitech Iberia, Samsung Electronics.
     * </p>
     */
    @Column(name = "nombre_empresa", nullable = false)
    private String nombreEmpresa;

    /**
     * Nombre de la persona de contacto en la empresa proveedora.
     */
    @Column(name = "contacto", nullable = false)
    private String contacto;

    /**
     * Número de teléfono del proveedor.
     */
    @Column(name = "telefono", nullable = false)
    private String telefono;

    /**
     * Correo electrónico del proveedor.
     */
    @Column(name = "email", nullable = false)
    private String email;

    /**
     * Dirección postal del proveedor.
     */
    @Column(name = "direccion", nullable = false)
    private String direccion;

    /**
     * Lista de productos suministrados habitualmente por el proveedor.
     *
     * <p>
     * Relación inversa: el FK {@code proveedor_id} vive en la tabla
     * {@code producto_inventario}. Esta lista se carga de forma
     * perezosa ({@code FetchType.LAZY}).
     * </p>
     *
     * <p>
     * <b>Serialización JSON</b>: Se utiliza {@code @JsonIgnore} para evitar
     * bucles infinitos de recursión (StackOverflowError) durante la conversión
     * a JSON, ya que {@link ProductoInventario} también referencia a esta entidad.
     * </p>
     *
     * <p>
     * {@code @Builder.Default} inicializa la lista como
     * {@code new ArrayList<>()}.
     * </p>
     */
    @OneToMany(mappedBy = "proveedor", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<ProductoInventario> productos = new ArrayList<>();

    /**
     * Lista de solicitudes de reposición dirigidas a este proveedor.
     *
     * <p>
     * Relación inversa: el FK {@code proveedor_id} vive en la tabla
     * {@code solicitud_reposicion}. Esta lista se carga de forma
     * perezosa ({@code FetchType.LAZY}).
     * </p>
     *
     * <p>
     * <b>Serialización JSON</b>: Se utiliza {@code @JsonIgnore} para evitar
     * bucles infinitos de recursión (StackOverflowError) durante la conversión
     * a JSON, ya que {@link SolicitudReposicion} también referencia a esta entidad.
     * </p>
     *
     * <p>
     * Las solicitudes mantienen su propio ciclo de vida y no se
     * eliminan al desactivar el proveedor.
     * </p>
     *
     * <p>
     * {@code @Builder.Default} inicializa la lista como
     * {@code new ArrayList<>()}.
     * </p>
     */
    @OneToMany(mappedBy = "proveedor", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<SolicitudReposicion> solicitudesReposicion = new ArrayList<>();

    /**
     * Indica si el proveedor se encuentra activo en el sistema.
     *
     * <p>
     * Se utiliza para el borrado lógico (soft delete). Los proveedores
     * desactivados permanecen en la base de datos pero no aparecen en
     * las consultas estándar ni como opción al crear solicitudes de
     * reposición.
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