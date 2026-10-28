package com.technovald.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envoltorio de paginación. Evita exponer el JSON de {@code Page}, que cambia
 * entre versiones de Spring Data.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PaginaResponse<T> {

    private List<T> contenido;
    private int pagina;
    private int tamano;
    private long totalElementos;
    private int totalPaginas;

    /**
     * Método estático para construir el DTO a partir del Page de Spring Data.
     */
    public static <T> PaginaResponse<T> de(Page<T> page) {
        return PaginaResponse.<T>builder()
                .contenido(page.getContent())
                .pagina(page.getNumber())
                .tamano(page.getSize())
                .totalElementos(page.getTotalElements())
                .totalPaginas(page.getTotalPages())
                .build();
    }
}