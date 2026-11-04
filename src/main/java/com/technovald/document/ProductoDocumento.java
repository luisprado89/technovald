package com.technovald.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ficha pública de un producto en MongoDB. Se enlaza con el inventario de
 * MySQL mediante el EAN.
 */
@Document(collection = "productos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoDocumento {

    /** Identificador único autogenerado por MongoDB. */
    @Id
    private String id;

    /** Código de barras EAN; puente con el inventario. */
    @Indexed(unique = true)
    private String ean;

    /** Título largo de Icecat, para el detalle. */
    private String titulo;

    /** Nombre corto, para las tarjetas del catálogo. */
    private String nombreCorto;

    /** Marca del producto. */
    private String marca;

    /** Categoría (texto libre). */
    private String categoria;

    /** Descripción de una línea, para la tarjeta. */
    private String descripcionCorta;

    /** Descripción larga, para el detalle. */
    private String descripcionLarga;

    /** Galería de imágenes. */
    @Builder.Default
    private List<Imagen> imagenes = new ArrayList<>();

    /** Especificaciones técnicas agrupadas. */
    @Builder.Default
    private List<EspecificacionGrupo> especificaciones = new ArrayList<>();

    /** Reseñas de clientes. */
    @Builder.Default
    private List<Comentario> comentarios = new ArrayList<>();

    /** Baja lógica de la ficha. */
    @Builder.Default
    private Boolean activo = true;

    /** Última sincronización con Icecat o alta manual. */
    private LocalDateTime fechaSincronizacion;
}