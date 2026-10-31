package com.technovald.service.impl;

import com.technovald.document.ProductoDocumento;
import com.technovald.dto.request.ProductoCatalogoRequest;
import com.technovald.dto.response.ProductoCatalogoResponse;
import com.technovald.entity.ProductoInventario;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.ProductoCatalogoMapper;
import com.technovald.repository.mongo.ProductoMongoRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Implementación de {@link CatalogoService}. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogoServiceImpl implements CatalogoService {

    private final ProductoMongoRepository productoMongoRepository;
    private final ProductoInventarioRepository productoInventarioRepository;
    private final ProductoCatalogoMapper productoCatalogoMapper;

    /** Devuelve todas las fichas de catálogo enriquecidas con precio y stock. */
    @Override
    public List<ProductoCatalogoResponse> listarProductosCatalogo() {
        List<ProductoDocumento> documentos = productoMongoRepository.findAllByOrderByNombreAsc();
        return productoCatalogoMapper.toResponseList(documentos, mapaInventarioPorEan(documentos));
    }

    /** Busca una ficha por su EAN o lanza una excepción si no existe. */
    @Override
    public ProductoCatalogoResponse obtenerProductoCatalogoPorEan(String ean) {
        String normalizado = normalizar(ean);
        ProductoDocumento documento = productoMongoRepository.findByEan(normalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto de catálogo no encontrado con EAN: " + normalizado));

        return productoCatalogoMapper.toResponse(documento, buscarInventarioPorEan(normalizado));
    }

    /**
     * Guarda una ficha de catálogo. Si ya existe una con el mismo EAN, la actualiza
     * (upsert); si no, la crea.
     */
    @Override
    public ProductoCatalogoResponse guardarProductoCatalogo(ProductoCatalogoRequest productoCatalogoRequest) {
        String ean = normalizar(productoCatalogoRequest.getEan());
        ProductoDocumento existente = productoMongoRepository.findByEan(ean).orElse(null);
        ProductoDocumento documento = productoCatalogoMapper.toDocumento(productoCatalogoRequest, existente);

        LocalDateTime ahora = LocalDateTime.now();
        if (documento.getFechaCreacion() == null) {
            documento.setFechaCreacion(ahora);
        }
        documento.setFechaActualizacion(ahora);

        ProductoDocumento guardado = productoMongoRepository.save(documento);
        log.info("Ficha de catálogo guardada: EAN {} (id {})", ean, guardado.getId());

        return productoCatalogoMapper.toResponse(guardado, buscarInventarioPorEan(ean));
    }

    /**
     * Devuelve un mapa de productos de MySQL indexado por su EAN.
     * Se usa para enriquecer listas grandes de fichas de catálogo sin hacer N+1 consultas.
     */
    private Map<String, ProductoInventario> mapaInventarioPorEan(List<ProductoDocumento> documentos) {
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
     * Busca un único producto en el inventario (MySQL) por su EAN.
     * Devuelve {@code null} si no existe en el inventario.
     */
    private ProductoInventario buscarInventarioPorEan(String ean) {
        return productoInventarioRepository.findByEanIgnoreCase(ean).orElse(null);
    }

    /** Limpia espacios y pasa el EAN a mayúsculas para evitar duplicados. */
    private String normalizar(String ean) {
        return ean.trim().toUpperCase(Locale.ROOT);
    }
}