package com.technovald.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Map;

/** Datos del alta o edición manual de una ficha. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoCatalogoRequest {

    /** EAN que identifica la ficha. */
    @NotBlank(message = "El EAN es obligatorio")
    @Size(max = 50, message = "El EAN no puede superar los 50 caracteres")
    private String ean;

    /** Título largo. */
    @NotBlank(message = "El título es obligatorio")
    @Size(max = 255, message = "El título no puede superar los 255 caracteres")
    private String titulo;

    /** Nombre corto; si falta, se usa el título. */
    @Size(max = 100, message = "El nombre corto no puede superar los 100 caracteres")
    private String nombreCorto;

    /** Marca. */
    @Size(max = 100, message = "La marca no puede superar los 100 caracteres")
    private String marca;

    /** Categoría. */
    @Size(max = 100, message = "La categoría no puede superar los 100 caracteres")
    private String categoria;

    /** Descripción corta. */
    @Size(max = 500, message = "La descripción corta no puede superar los 500 caracteres")
    private String descripcionCorta;

    /** Descripción larga. */
    @Size(max = 20000, message = "La descripción larga no puede superar los 20000 caracteres")
    private String descripcionLarga;

    /** URLs de imágenes. */
    private List<String> imagenes;

    /** Especificaciones en formato clave-valor (se guardan en un solo grupo). */
    private Map<String, String> especificaciones;

    /** Estado de la ficha; si falta, se conserva el actual. */
    private Boolean activo;
}