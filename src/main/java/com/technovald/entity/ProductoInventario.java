package com.technovald.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un producto del inventario en TechNovaLD
 * (MySQL).
 *
 * <p>
 * ProductoInventario es la entidad operativa del inventario: contiene
 * el stock actual, el precio de compra, el precio de venta, el stock
 * mínimo de alerta y la marca. Es distinta de
 * {@code ProductoDocumento} (MongoDB), que almacena la información rica
 * del catálogo (descripciones, imágenes, especificaciones técnicas)
 * sincronizada desde Icecat.
 * </p>
 *
 * <p>
 * La separación entre MySQL y MongoDB responde a dos necesidades:
 * </p>
 * <ul>
 *     <li><b>MySQL</b> ({@code ProductoInventario}): gestión interna
 *     del inventario (stock, precios, alertas, márgenes). Consultas
 *     rápidas y transaccionales.</li>
 *     <li><b>MongoDB</b> ({@code ProductoDocumento}): catálogo público
 *     con datos ricos de Icecat (imágenes, fichas técnicas). Consultas
 *     de lectura optimizadas para la web.</li>
 * </ul>
 *
 * <p>
 * El puente entre ambas bases de datos es el campo {@code ean}
 * (código de barras). Ambas entidades comparten el mismo EAN, lo que
 * permite sincronizar y enlazar los datos del catálogo (Mongo) con los
 * datos operativos del inventario (MySQL).
 * </p>
 *
 * <p>
 * El campo {@code marca} se almacena como {@code String} simple, no
 * como una entidad separada. La información rica de la marca (logo,
 * descripción, web) vive en el catálogo MongoDB, poblada por Icecat.
 * En el inventario MySQL solo se guarda el nombre de la marca para
 * filtros y reportes.
 * </p>
 *
 * <p>
 * El campo {@code precioVenta} es el precio de venta <b>actual</b>.
 * Cuando un cliente realiza un pedido, este precio se "congela" como
 * snapshot en {@link PedidoDetalle#getPrecioUnitario()}, de forma que
 * los pedidos históricos conservan el precio del momento de la compra
 * aunque el precio del producto cambie en el futuro.
 * </p>
 *
 * <p>
 * El campo {@code precioCompra} permite calcular el margen comercial
 * de cada producto ({@code precioVenta - precioCompra}), información
 * esencial para la gestión del inventario y los reportes de
 * rentabilidad.
 * </p>
 *
 * <p>
 * El campo {@code stockActual} representa las unidades disponibles en
 * el almacén. Cuando {@code stockActual < stockMinimo}, el sistema
 * debe sugerir crear una {@link SolicitudReposicion} al proveedor
 * correspondiente.
 * </p>
 *
 * <p>
 * ProductoInventario no declara {@code CascadeType} en ninguna
 * relación. Cada entidad mantiene su propio ciclo de vida.
 * </p>

 * <h3>Índices definidos</h3>
 * <ul>
 *   <li>{@code @Index(name = "idx_producto_activo", columnList = "activo")}:
 *       índice simple sobre {@code activo}. Cubre consultas que filtran
 *       solo por estado (listar todo el catálogo activo).</li>
 *   <li>{@code @Index(name = "idx_producto_categoria_activo", columnList = "categoria_id, activo")}:
 *       índice compuesto que optimiza búsquedas por categoría y estado
 *       simultáneamente ({@code WHERE categoria_id = ? AND activo = true}).</li>
 *   <li>{@code @Index(name = "idx_producto_proveedor_activo", columnList = "proveedor_id, activo")}:
 *       índice compuesto que optimiza búsquedas por proveedor y estado
 *       simultáneamente ({@code WHERE proveedor_id = ? AND activo = true}).</li>
 * </ul>
 *
 * <p><b>¿Por qué tres índices?</b> Cada uno responde a un patrón de consulta
 * real del inventario. El índice simple es necesario porque los compuestos
 * no sirven para filtrar <b>solo</b> por {@code activo}. En los compuestos
 * el orden importa: primero la columna más selectiva ({@code categoria_id}
 * o {@code proveedor_id}) y después {@code activo}, que solo tiene dos
 * valores. Todos incluyen {@code activo} porque el soft delete es
 * transversal al sistema.</p>
 *
 * <p><b>Nota:</b> Los índices aceleran las lecturas ({@code SELECT}) a cambio
 * de un leve costo en escrituras ({@code INSERT}/{@code UPDATE}/{@code DELETE})
 * y espacio en disco. Por eso se limitan a columnas realmente usadas en
 * {@code WHERE}, {@code JOIN} u {@code ORDER BY}, evitando índices
 * redundantes.</p>
 *
 * @see jakarta.persistence.Index
 * @see Categoria
 * @see Proveedor
 * @see SolicitudReposicion
 */
@Entity
@Table(name = "producto_inventario", indexes = {
        @Index(name = "idx_producto_activo", columnList = "activo"),
        @Index(name = "idx_producto_categoria_activo", columnList = "categoria_id, activo"),
        @Index(name = "idx_producto_proveedor_activo", columnList = "proveedor_id, activo")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoInventario {

    /**
     * Identificador único del producto en el inventario.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código de barras EAN (European Article Number) del producto.
     *
     * <p>
     * Es el <b>puente</b> entre MySQL (inventario) y MongoDB (catálogo).
     * Ambas bases de datos comparten el mismo EAN para el mismo
     * producto, lo que permite sincronizar y enlazar los datos del
     * catálogo con los datos operativos del inventario.
     * </p>
     *
     * <p>
     * Restricción {@code unique = true} a nivel de columna para
     * garantizar que no existan dos productos con el mismo EAN.
     * </p>
     */
    @Column(nullable = false, unique = true)
    private String ean;

    /**
     * Nombre corto del producto para búsquedas transaccionales.
     *
     * <p>
     * Es el nombre operativo usado en la gestión interna del
     * inventario (listados, reportes, alertas). El nombre rico para
     * mostrar en el catálogo público se obtiene del documento
     * MongoDB ({@code ProductoDocumento.nombre}).
     * </p>
     */
    @Column(name = "nombre_corto", nullable = false)
    private String nombreCorto;

    /**
     * Cantidad de unidades disponibles en el almacén.
     *
     * <p>
     * Cuando un cliente realiza un pedido, el servicio valida que
     * {@code stockActual >= cantidad solicitada}. Si no hay stock
     * suficiente, lanza una {@code BusinessRuleException}.
     * </p>
     *
     * <p>
     * Cuando una {@link SolicitudReposicion} pasa a estado
     * {@code RECIBIDO}, el servicio incrementa este campo en la
     * cantidad indicada en la solicitud.
     * </p>
     */
    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    /**
     * Umbral mínimo de stock para alertas de reposición.
     *
     * <p>
     * Cuando {@code stockActual < stockMinimo}, el sistema debe sugerir
     * crear una {@link SolicitudReposicion} al proveedor para
     * reponer el inventario.
     * </p>
     */
    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    /**
     * Precio de compra del producto al proveedor.
     *
     * <p>
     * Representa el coste de adquisición de una unidad del producto.
     * Se utiliza para calcular el margen comercial:
     * {@code margen = precioVenta - precioCompra}.
     * </p>
     *
     * <p>
     * {@code precision = 10, scale = 2} permite almacenar importes
     * de hasta 99.999.999,99.
     * </p>
     */
    @Column(name = "precio_compra", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCompra;

    /**
     * Precio de venta actual del producto al cliente.
     *
     * <p>
     * Es el precio vigente en el inventario. Cuando un cliente realiza
     * un pedido, este precio se "congela" como snapshot en
     * {@link PedidoDetalle#getPrecioUnitario()} para que los pedidos
     * históricos conserven el precio del momento de la compra aunque
     * el precio del producto cambie en el futuro.
     * </p>
     *
     * <p>
     * {@code precision = 10, scale = 2} permite almacenar importes
     * de hasta 99.999.999,99.
     * </p>
     */
    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;

    /**
     * Marca del producto (ej: Samsung, Logitech, Apple).
     *
     * <p>
     * Se almacena como {@code String} simple, no como una entidad
     * separada. La información rica de la marca (logo, descripción,
     * web) vive en el catálogo MongoDB, poblada por Icecat. En el
     * inventario MySQL solo se guarda el nombre para filtros y
     * reportes.
     * </p>
     *
     * <p>
     * Se rellena automáticamente cuando el administrador crea un
     * ProductoInventario a partir de un ProductoDocumento (Mongo):
     * el mapper extrae el nombre de la marca del documento y lo
     * vuelca al inventario.
     * </p>
     */
    @Column(name = "marca", nullable = false, length = 100)
    private String marca;

    /**
     * Categoría a la que pertenece el producto.
     *
     * <p>
     * ProductoInventario es el dueño de la relación: el FK
     * {@code categoria_id} vive en la tabla
     * {@code producto_inventario}. Todo producto debe tener una
     * categoría asociada.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar los datos de la
     * categoría cuando solo se necesita información del producto.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /**
     * Proveedor habitual del producto.
     *
     * <p>
     * ProductoInventario es el dueño de la relación: el FK
     * {@code proveedor_id} vive en la tabla
     * {@code producto_inventario}. Todo producto debe tener un
     * proveedor asociado.
     * </p>
     *
     * <p>
     * Es el proveedor por defecto para solicitar reposiciones. Sin
     * embargo, una {@link SolicitudReposicion} puede indicar un
     * proveedor distinto en casos excepcionales.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar los datos del
     * proveedor cuando solo se necesita información del producto.
     * </p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    /**
     * Lista de solicitudes de reposición asociadas al producto.
     *
     * <p>
     * Relación inversa: el FK {@code producto_id} vive en la tabla
     * {@code solicitud_reposicion}. Esta lista se carga de forma
     * perezosa ({@code FetchType.LAZY}).
     * </p>
     *
     * <p>
     * Las solicitudes mantienen su propio ciclo de vida y no se
     * eliminan al desactivar el producto.
     * </p>
     *
     * <p>
     * <b>Serialización JSON</b>: Se utiliza {@code @JsonIgnore} para evitar
     * bucles infinitos de recursión (StackOverflowError) durante la conversión
     * a JSON, ya que {@link SolicitudReposicion} también referencia a esta entidad.
     * </p>
     *
     * <p>
     * {@code @Builder.Default} inicializa la lista como
     * {@code new ArrayList<>()}.
     * </p>
     */
    @OneToMany(mappedBy = "producto", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<SolicitudReposicion> solicitudesReposicion = new ArrayList<>();

    /**
     * Indica si el producto se encuentra activo en el inventario.
     *
     * <p>
     * Se utiliza para el borrado lógico (soft delete). Los productos
     * desactivados permanecen en la base de datos para auditoría pero
     * no aparecen en las consultas estándar ni en el catálogo.
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
