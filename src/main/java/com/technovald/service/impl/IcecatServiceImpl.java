package com.technovald.service.impl;

import com.technovald.client.IcecatClient;
import com.technovald.document.ProductoDocumento;
import com.technovald.dto.icecat.IcecatResponse;
import com.technovald.dto.response.ProductoCatalogoResponse;
import com.technovald.dto.response.SincronizacionResponse;
import com.technovald.entity.ProductoInventario;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.IcecatMapper;
import com.technovald.mapper.ProductoCatalogoMapper;
import com.technovald.repository.mongo.ProductoMongoRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.service.IcecatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implementación de {@link IcecatService}.
 *
 * <p>
 * Hace upsert por EAN en MongoDB: si el documento existe conserva su
 * {@code id} y su {@code fechaCreacion}; la {@code fechaActualizacion} se
 * refresca en cada sincronización.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IcecatServiceImpl implements IcecatService {

    private final IcecatClient icecatClient;
    private final IcecatMapper icecatMapper;
    private final ProductoMongoRepository productoMongoRepository;
    private final ProductoInventarioRepository productoInventarioRepository;
    private final ProductoCatalogoMapper productoCatalogoMapper;

    /**
     * Sincroniza la ficha de un EAN y la guarda en MongoDB.
     *
     * @param ean código de barras del producto.
     * @return la ficha guardada, con precio y stock del inventario.
     * @throws ResourceNotFoundException si el EAN no existe en MySQL.
     * @throws com.technovald.exception.IcecatApiException si Icecat falla.
     */
    @Override
    public ProductoCatalogoResponse sincronizarProducto(String ean) {
        String normalizado = ean.trim().toUpperCase(Locale.ROOT);

        ProductoInventario inventario = productoInventarioRepository.findByEanIgnoreCase(normalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay producto en el inventario con EAN: " + normalizado));

        IcecatResponse respuesta = icecatClient.obtenerProducto(normalizado);
        ProductoDocumento existente = productoMongoRepository.findByEan(normalizado).orElse(null);
        ProductoDocumento documento = icecatMapper.toDocumento(respuesta, normalizado, existente);

        LocalDateTime ahora = LocalDateTime.now();
        if (documento.getFechaCreacion() == null) {
            documento.setFechaCreacion(ahora);
        }
        documento.setFechaActualizacion(ahora);

        ProductoDocumento guardado = productoMongoRepository.save(documento);
        log.info("Ficha sincronizada desde Icecat: EAN {}", normalizado);

        return productoCatalogoMapper.toResponse(guardado, inventario);
    }

    /**
     * Recorre todo el inventario y sincroniza cada EAN. Un fallo individual
     * se registra y se acumula, pero no detiene el resto.
     *
     * @return el resumen con total, sincronizados, fallidos y errores.
     */
    @Override
    public SincronizacionResponse sincronizarInventario() {
        List<ProductoInventario> productos = productoInventarioRepository.findAllByOrderByNombreCortoAsc();
        List<String> errores = new ArrayList<>();
        int sincronizados = 0;

        for (ProductoInventario producto : productos) {
            try {
                sincronizarProducto(producto.getEan());
                sincronizados++;
            } catch (RuntimeException ex) {
                log.warn("Fallo al sincronizar el EAN {}: {}", producto.getEan(), ex.getMessage());
                errores.add(producto.getEan() + ": " + ex.getMessage());
            }
        }

        log.info("Sincronización Icecat terminada: {} de {} productos",
                sincronizados, productos.size());
        return new SincronizacionResponse(productos.size(), sincronizados, errores.size(), errores);
    }
}