package com.technovald.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad maestra que representa una categoría de productos en
 * TechNovaLD.
 *
 * <p>
 * Las categorías agrupan los productos del inventario por tipo
 * (ej: Portátiles, Ratones, Teclados, Monitores). Son gestionadas
 * por el administrador y sirven como filtro en el catálogo público.
 * </p>
 *
 * <p>
 * Una categoría puede tener asociados múltiples
 * {@link ProductoInventario}. La relación es 1:N, donde el FK
 * {@code categoria_id} vive en la tabla {@code producto_inventario}.
 * </p>
 *
 * <p>
 * Categoria no declara {@code CascadeType} en la relación con
 * ProductoInventario. Los productos mantienen su propio ciclo de vida
 * y no se eliminan al desactivar una categoría.
 * </p>
 *
 * <p><b>Índice:</b> definido con {@code @Index} usando {@code name = "idx_categoria_activo"}
 * y {@code columnList = "activo"}, para optimizar búsquedas frecuentes por estado
 * (categorías activas/inactivas).</p>
 *
 * <p><b>Nota:</b> Los índices aceleran las lecturas ({@code SELECT}) a cambio de
 * un leve costo en escrituras ({@code INSERT}/{@code UPDATE}/{@code DELETE}) y
 * espacio en disco. Solo deben aplicarse sobre columnas usadas frecuentemente
 * en {@code WHERE}, {@code JOIN} u {@code ORDER BY}.</p>
 *
 * @see jakarta.persistence.Index
 */
@Entity
@Table(name = "categoria", indexes = {
        @Index(name = "idx_categoria_activo", columnList = "activo")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Categoria {

    /**
     * Identificador único de la categoría.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de la categoría.
     *
     * <p>
     * Debe ser único en el sistema. Se valida en el DTO de request
     * y se persiste en la columna {@code nombre} con restricción
     * {@code unique = true}.
     * </p>
     */
    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    /**
     * Descripción opcional de la categoría.
     *
     * <p>
     * Campo opcional que permite añadir información adicional sobre
     * el tipo de productos que agrupa la categoría.
     * </p>
     */
    @Column(name = "descripcion")
    private String descripcion;

    /**
     * Lista de productos asociados a la categoría.
     *
     * <p>
     * Relación inversa: el FK {@code categoria_id} vive en la tabla
     * {@code producto_inventario}. Esta lista se carga de forma
     * perezosa ({@code FetchType.LAZY}) para evitar recuperar todos
     * los productos de la categoría cuando solo se necesita información
     * de la propia categoría.
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
     * {@code new ArrayList<>()}. Sin esta anotación, Lombok Builder
     * dejaría el campo en {@code null}.
     * </p>
     */
    @OneToMany(mappedBy = "categoria", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<ProductoInventario> productos = new ArrayList<>();

    /**
     * Indica si la categoría se encuentra activa en el sistema.
     *
     * <p>
     * Se utiliza para el borrado lógico (soft delete). Las categorías
     * desactivadas permanecen en la base de datos pero no aparecen en
     * las consultas estándar ni en el catálogo público.
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
