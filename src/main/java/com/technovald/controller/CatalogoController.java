package com.technovald.controller;

import com.technovald.dto.response.ProductoCatalogoResponse;
import com.technovald.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Controlador REST del catálogo público (accesible sin autenticación). */
@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoController {

    private final CatalogoService catalogoService;

    /**
     * Devuelve todos los productos del catálogo.
     *
     * @return lista de fichas públicas.
     */
    @GetMapping
    public ResponseEntity<List<ProductoCatalogoResponse>> listarProductosCatalogo() {
        return ResponseEntity.ok(catalogoService.listarProductosCatalogo());
    }

    /**
     * Devuelve el detalle de un producto por su EAN.
     *
     * @param ean código de barras del producto.
     * @return la ficha pública del producto.
     */
    @GetMapping("/{ean}")
    public ResponseEntity<ProductoCatalogoResponse> detalleProductoCatalogo(@PathVariable String ean) {
        return ResponseEntity.ok(catalogoService.obtenerProductoCatalogoPorEan(ean));
    }
}