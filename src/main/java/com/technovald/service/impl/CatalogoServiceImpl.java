package com.technovald.service.impl;

import com.technovald.document.ProductoDocumento;
import com.technovald.dto.request.ProductoCatalogoRequest;
import com.technovald.dto.response.ProductoCatalogoDetalleResponse;
import com.technovald.dto.response.ProductoCatalogoResumenResponse;
import com.technovald.entity.ProductoInventario;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.ProductoCatalogoMapper;
import com.technovald.repository.mongo.ProductoDocumentoUpdates;
import com.technovald.repository.mongo.ProductoMongoRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación de {@link CatalogoService}.
 *
 * <p>
 * El listado devuelve solo las fichas activas y se enriquece con el precio y el
 * stock de MySQL en una sola consulta. El detalle incluye descripciones,
 * galería, especificaciones y reseñas. El alta manual hace upsert por EAN y
 * conserva los comentarios y el estado de una ficha existente.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogoServiceImpl implements CatalogoService {

    private final ProductoMongoRepository productoMongoRepository;
    private final ProductoInventarioRepository productoInventarioRepository;
    private final ProductoCatalogoMapper productoCatalogoMapper;
    private final MongoTemplate mongoTemplate;

    /**
     * Devuelve las fichas activas para las tarjetas del catálogo.
     *
     * <p>
     * El precio y el stock se resuelven con una única consulta a MySQL, para no
     * hacer una consulta por cada ficha del listado.
     * </p>
     *
     * @return las fichas activas ordenadas por nombre corto.
     */
    @Override
    public List<ProductoCatalogoResumenResponse> listarProductosCatalogo() {
        List<ProductoDocumento> documentos = productoMongoRepository.findAllByActivoTrueOrderByNombreCortoAsc();
        return productoCatalogoMapper.toResumenList(documentos, mapaInventario(documentos));
    }

    /**
     * Obtiene la ficha completa de un producto activo.
     *
     * @param ean código de barras del producto.
     * @return la ficha con descripciones, galería, especificaciones y reseñas.
     * @throws ResourceNotFoundException si no existe una ficha activa con ese EAN.
     */
    @Override
    public ProductoCatalogoDetalleResponse obtenerProductoCatalogoPorEan(String ean) {
        String normalizado = normalizar(ean);
        ProductoDocumento documento = productoMongoRepository.findByEanAndActivoTrue(normalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto de catálogo no encontrado con EAN: " + normalizado));

        return productoCatalogoMapper.toDetalle(documento, buscarInventario(normalizado));
    }

    /**
     * Crea o actualiza una ficha manualmente.
     *
     * <p>
     * Si la ficha ya existe se conservan sus comentarios y, si la petición no
     * indica lo contrario, su estado. La fecha de sincronización se actualiza en
     * cada guardado.
     * </p>
     *
     * @param request datos de la ficha.
     * @return la ficha completa guardada.
     */
    @Override
    public ProductoCatalogoDetalleResponse guardarProductoCatalogo(ProductoCatalogoRequest request) {
        String ean = normalizar(request.getEan());
        ProductoDocumento existente = productoMongoRepository.findByEan(ean).orElse(null);
        ProductoDocumento documento = productoCatalogoMapper.toDocumento(request, existente);
        documento.setFechaSincronizacion(LocalDateTime.now());

        mongoTemplate.upsert(
                Query.query(Criteria.where("ean").is(ean)),
                ProductoDocumentoUpdates.sinComentarios(mongoTemplate, documento),
                ProductoDocumento.class);

        // Relectura para obtener el _id generado en un alta nueva.
        ProductoDocumento guardado = productoMongoRepository.findByEan(ean)
                .orElseThrow(() -> new IllegalStateException(
                        "La ficha de catálogo no se pudo recuperar tras el upsert. EAN: " + ean));

        log.info("Ficha de catálogo guardada: EAN {} (id {})", ean, guardado.getId());

        return productoCatalogoMapper.toDetalle(guardado, buscarInventario(ean));
    }

    /**
     * Construye el mapa de productos de MySQL indexado por EAN.
     *
     * @param documentos fichas del catálogo.
     * @return el mapa EAN → producto, vacío si no hay fichas.
     */
    private Map<String, ProductoInventario> mapaInventario(List<ProductoDocumento> documentos) {
        List<String> eans = documentos.stream().map(ProductoDocumento::getEan).toList();
        if (eans.isEmpty()) {
            return Map.of();
        }
        return productoInventarioRepository.findAllByEanIn(eans).stream()
                .collect(Collectors.toMap(
                        producto -> producto.getEan().toUpperCase(Locale.ROOT),
                        producto -> producto,
                        (primero, segundo) -> primero));
    }

    /**
     * Busca el producto de inventario asociado a un EAN.
     *
     * @param ean EAN ya normalizado a mayúsculas.
     * @return el producto, o {@code null} si no está en el inventario.
     */
    private ProductoInventario buscarInventario(String ean) {
        return productoInventarioRepository.findByEan(ean).orElse(null);
    }

    /**
     * Normaliza el EAN a mayúsculas y sin espacios.
     *
     * @param ean EAN original.
     * @return el EAN normalizado.
     */
    private String normalizar(String ean) {
        return ean.trim().toUpperCase(Locale.ROOT);
    }
}
