package com.technovald.repository.mongo;

import com.technovald.document.ProductoDocumento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Repositorio de MongoDB para las fichas de catálogo. */
@Repository
public interface ProductoMongoRepository extends MongoRepository<ProductoDocumento, String> {

    /** Busca una ficha por su EAN (único). */
    Optional<ProductoDocumento> findByEan(String ean);

    /** Lista todas las fichas ordenadas alfabéticamente por nombre. */
    List<ProductoDocumento> findAllByOrderByNombreAsc();
}