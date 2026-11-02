package com.technovald.controller;

import com.technovald.dto.request.ProductoCatalogoRequest;
import com.technovald.dto.response.ProductoCatalogoResponse;
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

/**
 * Fichas de catálogo y sincronización con Icecat.
 *
 * <p>
 * El alta manual es un upsert por EAN; la sincronización consulta Icecat y
 * guarda el resultado en MongoDB con el mismo criterio. Las tres rutas
 * cuelgan de {@code /api/admin/catalogo}, que {@code SecurityConfig} reserva
 * a ADMIN, así que el controlador no repite la comprobación.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/catalogo")
@RequiredArgsConstructor
public class AdminCatalogoController {

    private final CatalogoService catalogoService;
    private final IcecatService icecatService;

    /**
     * Crea o actualiza una ficha de catálogo sin pasar por Icecat.
     *
     * @param productoCatalogoRequest datos de la ficha; el EAN identifica el documento.
     * @return la ficha guardada, con precio y stock del inventario.
     */
    @PostMapping
    public ResponseEntity<ProductoCatalogoResponse> guardarProductoCatalogo(
            @Valid @RequestBody ProductoCatalogoRequest productoCatalogoRequest) {

        return ResponseEntity.ok(catalogoService.guardarProductoCatalogo(productoCatalogoRequest));
    }

    /**
     * Sincroniza una ficha concreta desde Icecat.
     *
     * @param ean código de barras del producto; debe existir en el inventario.
     * @return la ficha guardada, con precio y stock del inventario.
     */
    @PostMapping("/sync/{ean}")
    public ResponseEntity<ProductoCatalogoResponse> sincronizarProducto(@PathVariable String ean) {
        return ResponseEntity.ok(icecatService.sincronizarProducto(ean));
    }

    /**
     * Sincroniza todo el inventario con Icecat. Un fallo no detiene el resto.
     *
     * @return el resumen con total, sincronizados, fallidos y errores.
     */
    @PostMapping("/sync")
    public ResponseEntity<SincronizacionResponse> sincronizarInventario() {
        return ResponseEntity.ok(icecatService.sincronizarInventario());
    }
}