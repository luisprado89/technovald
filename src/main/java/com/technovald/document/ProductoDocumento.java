package com.technovald.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Documento de MongoDB que representa la ficha pública de un producto en el catálogo.
 * Se enlaza con la tabla de inventario de MySQL mediante el EAN.
 */
@Document(collection = "productos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoDocumento {

    /**
     * Identificador único autogenerado por MongoDB.
     * <p>
     * Se genera automáticamente como un
     * {@code ObjectId} al persistir el documento.
     * </p>
     * */
    @Id
    private String id;

    /** Código de barras EAN. Actúa como puente con la tabla de inventario (MySQL). */
    @Indexed(unique = true)
    private String ean;

    /** Nombre comercial del producto. */
    private String nombre;

    /** Descripción detallada para la ficha pública. */
    private String descripcionLarga;

    /** Marca del producto. */
    private String marca;

    /** Categoría (texto libre para el catálogo). */
    private String categoria;

    /** Lista de URLs de imágenes del producto. */
    @Builder.Default
    private List<String> imagenes = new ArrayList<>();

    /** Especificaciones técnicas en formato clave-valor (ej. "Peso" -> "120 g"). */
    @Builder.Default
    private Map<String, String> especificaciones = new LinkedHashMap<>();

    /** Fecha de creación de la ficha. */
    private LocalDateTime fechaCreacion;

    /** Fecha de la última actualización. */
    private LocalDateTime fechaActualizacion;
}