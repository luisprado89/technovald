package com.technovald.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Datos de entrada para crear o editar una categoría. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CategoriaRequest {

    /** Nombre de la categoría (único en el sistema, ej. "Electrónica"). */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    /** Breve descripción de la categoría (opcional). */
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;
}