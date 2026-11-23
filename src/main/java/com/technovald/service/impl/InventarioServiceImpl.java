package com.technovald.service.impl;

import com.technovald.dto.request.ProductoInventarioAjusteRequest;
import com.technovald.dto.request.ProductoInventarioRequest;
import com.technovald.dto.response.ProductoInventarioResponse;
import com.technovald.entity.Categoria;
import com.technovald.entity.ProductoInventario;
import com.technovald.entity.Proveedor;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.ProductoInventarioMapper;
import com.technovald.repository.mysql.CategoriaRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.repository.mysql.ProveedorRepository;
import com.technovald.service.InventarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Implementación de {@link InventarioService}. */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioServiceImpl implements InventarioService {

    private final ProductoInventarioRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoInventarioMapper productoMapper;

    /** Lista todos los productos, activos e inactivos. */
    @Override
    @Transactional(readOnly = true)
    public List<ProductoInventarioResponse> listarProductos() {
        return productoMapper.toResponseList(productoRepository.findAllByOrderByNombreCortoAsc());
    }

    /** Lista los productos activos con stock bajo. */
    @Override
    @Transactional(readOnly = true)
    public List<ProductoInventarioResponse> listarStockBajo() {
        return productoMapper.toResponseList(productoRepository.findLowStock());
    }

    /**
     * Crea un producto validando que el EAN sea único.
     */
    @Override
    @Transactional
    public ProductoInventarioResponse crearProducto(ProductoInventarioRequest productoInventarioRequest) {
        String ean = productoMapper.normalizarEan(productoInventarioRequest.getEan());

        if (productoRepository.existsByEan(ean)) {
            throw new BusinessRuleException("Ya existe un producto con ese EAN");
        }

        ProductoInventario producto = productoMapper.toEntity(productoInventarioRequest,
                categoria(productoInventarioRequest.getCategoriaId()), proveedor(productoInventarioRequest.getProveedorId()));
        productoRepository.save(producto);

        log.info("Producto creado: '{}' (id {}, EAN {})",
                producto.getNombreCorto(), producto.getId(), ean);
        return productoMapper.toResponse(producto);
    }

    /**
     * Actualiza los datos del producto (el EAN y el stock no se tocan).
     */
    @Override
    @Transactional
    public ProductoInventarioResponse actualizarProducto(Long id, ProductoInventarioRequest productoInventarioRequest) {
        ProductoInventario producto = productoPorId(id);

        productoMapper.updateEntity(producto, productoInventarioRequest,
                categoria(productoInventarioRequest.getCategoriaId()), proveedor(productoInventarioRequest.getProveedorId()));
        productoRepository.save(producto);

        log.info("Producto actualizado: id {}", id);
        return productoMapper.toResponse(producto);
    }

    /**
     * Ajusta el stock sumando la cantidad (puede ser negativa) y valida que no quede negativo.
     */
    @Override
    @Transactional
    public ProductoInventarioResponse ajustarStockProducto(Long id, ProductoInventarioAjusteRequest productoInventarioAjusteRequest) {
        ProductoInventario producto = productoPorId(id);
        int nuevoStock = producto.getStockActual() + productoInventarioAjusteRequest.getCantidad();

        if (nuevoStock < 0) {
            throw new BusinessRuleException("El stock resultante no puede ser negativo");
        }

        producto.setStockActual(nuevoStock);
        productoRepository.save(producto);

        log.info("Stock ajustado: producto {} -> {}", id, nuevoStock);
        return productoMapper.toResponse(producto);
    }

    /** Baja lógica: desactiva el producto. */
    @Override
    @Transactional
    public void desactivarProducto(Long id) {
        ProductoInventario producto = productoPorId(id);
        producto.setActivo(false);
        productoRepository.save(producto);
        log.info("Producto desactivado: id {}", id);
    }

    /** Reactiva un producto previamente desactivado. */
    @Override
    @Transactional
    public void reactivarProducto(Long id) {
        ProductoInventario producto = productoPorId(id);
        producto.setActivo(true);
        productoRepository.save(producto);
        log.info("Producto reactivado: id {}", id);
    }

    /** Busca un producto por ID o lanza una excepción si no existe. */
    private ProductoInventario productoPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }

    /** Busca una categoría por ID o lanza una excepción si no existe. */
    private Categoria categoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
    }

    /** Busca un proveedor por ID o lanza una excepción si no existe. */
    private Proveedor proveedor(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado con id: " + id));
    }
}