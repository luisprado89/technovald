package com.technovald.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Map;

/** Datos de entrada para crear o actualizar una ficha de catálogo. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductoCatalogoRequest {

    /** Código EAN que identifica la ficha (debe existir en el inventario). */
    @NotBlank(message = "El EAN es obligatorio")
    @Size(max = 50, message = "El EAN no puede superar los 50 caracteres")
    private String ean;

    /** Nombre comercial del producto. */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
    private String nombre;

    /** Descripción detallada (opcional). */
    @Size(max = 5000, message = "La descripción no puede superar los 5000 caracteres")
    private String descripcionLarga;

    /** Marca del producto. */
    @Size(max = 100, message = "La marca no puede superar los 100 caracteres")
    private String marca;

    /** Categoría del producto (texto libre). */
    @Size(max = 100, message = "La categoría no puede superar los 100 caracteres")
    private String categoria;

    /** Lista de URLs de imágenes. */
    private List<String> imagenes;

    /** Especificaciones técnicas en formato clave-valor. */
    private Map<String, String> especificaciones;
}