package com.technovald.service.impl;

import com.technovald.client.IcecatClient;
import com.technovald.document.ProductoDocumento;
import com.technovald.dto.icecat.IcecatResponse;
import com.technovald.dto.response.ProductoCatalogoDetalleResponse;
import com.technovald.dto.response.SincronizacionResponse;
import com.technovald.entity.ProductoInventario;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.IcecatMapper;
import com.technovald.mapper.ProductoCatalogoMapper;
import com.technovald.repository.mongo.ProductoDocumentoUpdates;
import com.technovald.repository.mongo.ProductoMongoRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.service.IcecatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implementación de {@link IcecatService}.
 *
 * <p>
 * Hace upsert por EAN en MongoDB. El EAN solicitado manda sobre el de Icecat
 * porque es el puente con el inventario; la fecha de sincronización se
 * actualiza en cada ejecución y el estado de la ficha se conserva.
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
    private final MongoTemplate mongoTemplate;

    /**
     * Sincroniza la ficha de un EAN y la guarda en MongoDB.
     *
     * @param ean código de barras del producto.
     * @return la ficha completa guardada.
     * @throws ResourceNotFoundException si el EAN no existe en MySQL.
     * @throws com.technovald.exception.IcecatApiException si Icecat falla.
     */
    @Override
    public ProductoCatalogoDetalleResponse sincronizarProducto(String ean) {
        String normalizado = ean.trim().toUpperCase(Locale.ROOT);

        ProductoInventario inventario = productoInventarioRepository.findByEan(normalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No hay producto en el inventario con EAN: " + normalizado));

        IcecatResponse respuesta = icecatClient.obtenerProducto(normalizado);
        ProductoDocumento existente = productoMongoRepository.findByEan(normalizado).orElse(null);
        ProductoDocumento documento = icecatMapper.toDocumento(respuesta, normalizado, existente);
        documento.setFechaSincronizacion(LocalDateTime.now());

        // $set sin _id ni comentarios: una sincronización concurrente con una
        // reseña no la borra.
        mongoTemplate.upsert(
                Query.query(Criteria.where("ean").is(normalizado)),
                ProductoDocumentoUpdates.sinComentarios(mongoTemplate, documento),
                ProductoDocumento.class);

        // Relectura para obtener el _id generado en un alta nueva.
        ProductoDocumento guardado = productoMongoRepository.findByEan(normalizado)
                .orElseThrow(() -> new IllegalStateException(
                        "No se pudo recuperar la ficha sincronizada. EAN: " + normalizado));

        log.info("Ficha sincronizada desde Icecat: EAN {}", normalizado);

        return productoCatalogoMapper.toDetalle(guardado, inventario);
    }

    /**
     * Recorre todo el inventario y sincroniza cada EAN. Un fallo individual se
     * registra y se acumula, pero no detiene el resto.
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

        log.info("Sincronización Icecat terminada: {} de {} productos", sincronizados, productos.size());
        return new SincronizacionResponse(productos.size(), sincronizados, errores.size(), errores);
    }
}