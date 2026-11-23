package com.technovald.repository.mysql;

import com.technovald.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repositorio JPA para {@link Categoria}. */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Todas las categorías, activas e inactivas, por nombre. */
    List<Categoria> findAllByOrderByNombreAsc();

    /** MySQL compara sin distinguir mayúsculas ni acentos por la collation. */
    boolean existsByNombre(String nombre);

    /** Para editar: el mismo nombre no choca consigo mismo. */
    boolean existsByNombreAndIdNot(String nombre, Long id);
}
