package com.technovald.controller;

import com.technovald.dto.response.ProductoCatalogoDetalleResponse;
import com.technovald.dto.response.ProductoCatalogoResumenResponse;
import com.technovald.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Catálogo público: sin autenticación. */
@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    /**
     * Listado ligero de fichas activas.
     *
     * @return las tarjetas del catálogo.
     */
    @GetMapping
    public ResponseEntity<List<ProductoCatalogoResumenResponse>> listarProductosCatalogo() {
        return ResponseEntity.ok(catalogoService.listarProductosCatalogo());
    }

    /**
     * Detalle completo de una ficha.
     *
     * @param ean código de barras del producto.
     * @return la ficha completa.
     */
    @GetMapping("/{ean}")
    public ResponseEntity<ProductoCatalogoDetalleResponse> detalleProductoCatalogo(@PathVariable String ean) {
        return ResponseEntity.ok(catalogoService.obtenerProductoCatalogoPorEan(ean));
    }
}