package com.technovald.repository.mongo;

import com.technovald.document.ProductoDocumento;
import com.technovald.dto.response.ComentarioPendienteResponse;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Repositorio de MongoDB para las fichas de catálogo. */
@Repository
public interface ProductoMongoRepository extends MongoRepository<ProductoDocumento, String> {

    /** Busca una ficha por EAN, esté activa o no (para el upsert). */
    Optional<ProductoDocumento> findByEan(String ean);

    /** Busca una ficha activa por EAN (detalle público). */
    Optional<ProductoDocumento> findByEanAndActivoTrue(String ean);

    /** Lista las fichas activas ordenadas por nombre corto. */
    List<ProductoDocumento> findAllByActivoTrueOrderByNombreCortoAsc();

    /** Ficha que contiene una reseña con ese UUID. */
    Optional<ProductoDocumento> findByComentariosId(String comentarioId);

    /**
     * Reseñas sin respuesta oficial, con el EAN y el nombre del producto.
     *
     * <p>
     * El pipeline desenreda el array, descarta las reseñas ya respondidas o
     * inactivas y las de fichas despublicadas, y proyecta el EAN y el nombre
     * corto (con el título como respaldo) del producto padre.
     * </p>
     *
     * @return las reseñas pendientes con su producto de origen.
     */
    @Aggregation(pipeline = {
            "{ $unwind: '$comentarios' }",
            "{ $match: { 'activo': true, 'comentarios.respuestaOficial': null, 'comentarios.activo': true } }",
            "{ $project: { " +
                    "_id: 0, " +
                    "comentarioId: '$comentarios._id', " +
                    "productoEan: '$ean', " +
                    "productoTitulo: { $ifNull: ['$nombreCorto', '$titulo'] }, " +
                    "nombreCliente: '$comentarios.nombreCliente', " +
                    "texto: '$comentarios.texto', " +
                    "valoracion: '$comentarios.valoracion', " +
                    "fechaCreacion: '$comentarios.fechaCreacion' " +
                    "}}"
    })
    List<ComentarioPendienteResponse> findComentariosPendientes();
}