package com.technovald.service.impl;

import com.technovald.dto.request.CategoriaRequest;
import com.technovald.dto.response.CategoriaResponse;
import com.technovald.entity.Categoria;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.CategoriaMapper;
import com.technovald.repository.mysql.CategoriaRepository;
import com.technovald.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Implementación de {@link CategoriaService}. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    /**
     * Devuelve el listado completo de categorías (activas e inactivas) ordenadas por nombre.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarCategorias() {
        return categoriaMapper.toResponseList(categoriaRepository.findAllByOrderByNombreAsc());
    }

    /**
     * Crea una nueva categoría. Valida que el nombre no exista previamente.
     *
     * @throws BusinessRuleException si ya hay una categoría con ese nombre.
     */
    @Override
    @Transactional
    public CategoriaResponse crearCategoria(CategoriaRequest categoriaRequest) {
        String nombre = categoriaRequest.getNombre().trim();

        if (categoriaRepository.existsByNombre(nombre)) {
            throw new BusinessRuleException("Ya existe una categoría con ese nombre");
        }

        Categoria categoria = Categoria.builder()
                .nombre(nombre)
                .descripcion(textoOpcional(categoriaRequest.getDescripcion()))
                .activo(true)
                .build();
        categoriaRepository.save(categoria);

        log.info("Categoría creada: '{}' (id {})", nombre, categoria.getId());
        return categoriaMapper.toResponse(categoria);
    }

    /**
     * Actualiza el nombre y la descripción de una categoría existente.
     * Valida que el nombre no esté ya ocupado por otra categoría.
     *
     * @throws BusinessRuleException si el nuevo nombre ya existe en otra categoría.
     */
    @Override
    @Transactional
    public CategoriaResponse actualizarCategoria(Long id, CategoriaRequest categoriaRequest) {
        Categoria categoria = categoriaPorId(id);
        String nombre = categoriaRequest.getNombre().trim();

        if (categoriaRepository.existsByNombreAndIdNot(nombre, id)) {
            throw new BusinessRuleException("Ya existe una categoría con ese nombre");
        }

        categoria.setNombre(nombre);
        categoria.setDescripcion(textoOpcional(categoriaRequest.getDescripcion()));
        categoriaRepository.save(categoria);

        log.info("Categoría actualizada: '{}' (id {})", nombre, id);
        return categoriaMapper.toResponse(categoria);
    }

    /**
     * Baja lógica: desactiva la categoría sin eliminarla de la base de datos.
     */
    @Override
    @Transactional
    public void desactivarCategoria(Long id) {
        Categoria categoria = categoriaPorId(id);
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
        log.info("Categoría desactivada: id {}", id);
    }

    /**
     * Reactiva una categoría previamente dada de baja.
     */
    @Override
    @Transactional
    public void reactivarCategoria(Long id) {
        Categoria categoria = categoriaPorId(id);
        categoria.setActivo(true);
        categoriaRepository.save(categoria);
        log.info("Categoría reactivada: id {}", id);
    }

    /**
     * Busca una categoría por su ID o lanza una excepción si no existe.
     */
    private Categoria categoriaPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
    }

    /**
     * Normaliza campos de texto opcionales: si están vacíos o en blanco, los
     * convierte a {@code null} para que la base de datos no guarde cadenas vacías.
     */
    private String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}