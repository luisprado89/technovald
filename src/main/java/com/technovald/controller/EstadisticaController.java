package com.technovald.controller;

import com.technovald.dto.response.ProductoMasVendidoResponse;
import com.technovald.service.EstadisticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Estadísticas de ventas. La autorización se aplica por URL en
 * {@code SecurityConfig}; solo ADMIN puede consultarlas.
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    /**
     * Top 5 de productos por unidades vendidas.
     *
     * @return los productos ordenados de más a menos ventas.
     */
    @GetMapping("/productos-mas-vendidos")
    public ResponseEntity<List<ProductoMasVendidoResponse>> getProductosMasVendidos() {
        return ResponseEntity.ok(estadisticaService.getProductosMasVendidos());
    }
}