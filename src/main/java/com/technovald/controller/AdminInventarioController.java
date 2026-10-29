package com.technovald.controller;

import com.technovald.dto.request.ProductoInventarioAjusteRequest;
import com.technovald.dto.request.ProductoInventarioRequest;
import com.technovald.dto.response.ProductoInventarioResponse;
import com.technovald.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la gestión del inventario.
 *
 * <p>
 * Expone el CRUD de productos y el ajuste de stock. Las reglas de seguridad
 * (quién puede hacer qué) viven en {@code SecurityConfig}: la lectura y el
 * ajuste de stock están permitidos a ADMIN y TRABAJADOR, mientras que la
 * creación, edición y baja de productos es exclusiva de ADMIN.
 * </p>
 */
@RestController
@RequestMapping("/api/admin/inventario")
@RequiredArgsConstructor
public class AdminInventarioController {

    private final InventarioService inventarioService;

    /**
     * Devuelve todos los productos del inventario, incluidos los inactivos.
     *
     * @return lista completa de productos.
     */
    @GetMapping
    public ResponseEntity<List<ProductoInventarioResponse>> listarProductos() {
        return ResponseEntity.ok(inventarioService.listarProductos());
    }

    /**
     * Devuelve los productos activos con stock igual o inferior al mínimo.
     *
     * <p>Es la consulta que alimenta las alertas de reposición del panel.</p>
     *
     * @return lista de productos con stock bajo.
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductoInventarioResponse>> listarStockBajo() {
        return ResponseEntity.ok(inventarioService.listarStockBajo());
    }

    /**
     * Da de alta un producto en el inventario.
     *
     * <p>Responde {@code 201 Created} sin cabecera {@code Location} porque no
     * existe un endpoint de consulta individual por ID.</p>
     *
     * @param productoInventarioRequest datos del nuevo producto.
     * @return el producto creado.
     */
    @PostMapping
    public ResponseEntity<ProductoInventarioResponse> crearProducto(
            @Valid @RequestBody ProductoInventarioRequest productoInventarioRequest) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventarioService.crearProducto(productoInventarioRequest));
    }

    /**
     * Actualiza los datos de un producto existente.
     *
     * <p>El EAN y el stock actual son inmutables por esta vía: el stock se
     * modifica mediante el endpoint de ajuste.</p>
     *
     * @param id identificador del producto.
     * @param productoInventarioRequest nuevos datos del producto.
     * @return el producto actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductoInventarioResponse> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoInventarioRequest productoInventarioRequest) {

        return ResponseEntity.ok(inventarioService.actualizarProducto(id, productoInventarioRequest));
    }

    /**
     * Ajusta el stock de un producto sumando la cantidad indicada.
     *
     * <p>La cantidad puede ser positiva (entrada de mercancía) o negativa
     * (salida, merma o corrección). Si el stock resultante fuera negativo, el
     * servicio responde con un error 400.</p>
     *
     * @param id identificador del producto.
     * @param productoInventarioAjusteRequest cantidad a sumar o restar.
     * @return el producto con el stock actualizado.
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductoInventarioResponse> ajustarStockProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoInventarioAjusteRequest productoInventarioAjusteRequest) {

        return ResponseEntity.ok(inventarioService.ajustarStockProducto(id, productoInventarioAjusteRequest));
    }

    /**
     * Realiza la baja lógica de un producto.
     *
     * <p>El producto no se elimina de la base de datos; solo se marca como
     * inactivo para que deje de aparecer en las operaciones de venta.</p>
     *
     * @param id identificador del producto a desactivar.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarProducto(@PathVariable Long id) {
        inventarioService.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva un producto previamente dado de baja.
     *
     * <p>Vuelve a habilitar el producto para que pueda venderse y aparecer
     * en las consultas de stock.</p>
     *
     * @param id identificador del producto a reactivar.
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivarProducto(@PathVariable Long id) {
        inventarioService.reactivarProducto(id);
        return ResponseEntity.noContent().build();
    }
}