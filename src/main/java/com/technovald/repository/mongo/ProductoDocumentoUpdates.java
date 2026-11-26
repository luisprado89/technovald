package com.technovald.repository.mongo;

import com.technovald.document.ProductoDocumento;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;

/**
 * Construye el {@code $set} de una ficha de catálogo excluyendo {@code _id} y
 * {@code comentarios}.
 */
public final class ProductoDocumentoUpdates {

    private ProductoDocumentoUpdates() {
    }

    /**
     * Devuelve un {@code $set} con todos los campos de la ficha menos {@code _id} y {@code comentarios}.
     *
     * @param mongoTemplate plantilla de la que se toma el converter.
     * @param documento     ficha con los valores a persistir.
     * @return el {@code Update} listo para un upsert.
     */
    public static Update sinComentarios(MongoTemplate mongoTemplate, ProductoDocumento documento) {
        Document campos = new Document();
        mongoTemplate.getConverter().write(documento, campos);
        campos.remove("_id");
        campos.remove("comentarios");

        Update update = new Update();
        campos.forEach(update::set);
        return update;
    }
}
