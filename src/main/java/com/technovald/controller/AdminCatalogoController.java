package com.technovald.controller;

import com.technovald.dto.request.ProductoCatalogoRequest;
import com.technovald.dto.response.ProductoCatalogoDetalleResponse;
import com.technovald.dto.response.SincronizacionResponse;
import com.technovald.service.CatalogoService;
import com.technovald.service.IcecatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fichas de catálogo y sincronización con Icecat: solo ADMIN. */
@RestController
@RequestMapping("/api/admin/catalogo")
@RequiredArgsConstructor
public class AdminCatalogoController {

    private final CatalogoService catalogoService;
    private final IcecatService icecatService;

    /**
     * Crea o actualiza una ficha manualmente.
     *
     * @param productoCatalogoRequest datos de la ficha.
     * @return la ficha completa guardada.
     */
    @PostMapping
    public ResponseEntity<ProductoCatalogoDetalleResponse> guardarProductoCatalogo(
            @Valid @RequestBody ProductoCatalogoRequest productoCatalogoRequest) {

        return ResponseEntity.ok(catalogoService.guardarProductoCatalogo(productoCatalogoRequest));
    }

    /**
     * Sincroniza una ficha desde Icecat.
     *
     * @param ean EAN que debe existir en el inventario.
     * @return la ficha completa guardada.
     */
    @PostMapping("/sync/{ean}")
    public ResponseEntity<ProductoCatalogoDetalleResponse> sincronizarProducto(@PathVariable String ean) {
        return ResponseEntity.ok(icecatService.sincronizarProducto(ean));
    }

    /**
     * Sincroniza todo el inventario.
     *
     * @return el resumen con total, sincronizados, fallidos y errores.
     */
    @PostMapping("/sync")
    public ResponseEntity<SincronizacionResponse> sincronizarInventario() {
        return ResponseEntity.ok(icecatService.sincronizarInventario());
    }
}