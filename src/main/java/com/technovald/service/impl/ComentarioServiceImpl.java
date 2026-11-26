package com.technovald.service.impl;

import com.technovald.document.Comentario;
import com.technovald.document.ProductoDocumento;
import com.technovald.dto.request.ComentarioRequest;
import com.technovald.dto.request.RespuestaComentarioRequest;
import com.technovald.dto.response.ComentarioPendienteResponse;
import com.technovald.dto.response.ComentarioResponse;
import com.technovald.entity.Cliente;
import com.technovald.entity.ProductoInventario;
import com.technovald.entity.Usuario;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.ComentarioMapper;
import com.technovald.repository.mongo.ProductoMongoRepository;
import com.technovald.repository.mysql.ClienteRepository;
import com.technovald.repository.mysql.ProductoInventarioRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.service.ComentarioService;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación de {@link ComentarioService}.
 *
 * <p>
 * Las reseñas viven dentro del documento del producto en MongoDB. La
 * transacción es la de JPA y sirve para las lecturas de MySQL (usuario,
 * cliente, producto). Las escrituras usan operadores atómicos.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComentarioServiceImpl implements ComentarioService {

    private final ProductoMongoRepository productoMongoRepository;
    private final ProductoInventarioRepository productoInventarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final ComentarioMapper comentarioMapper;
    private final MongoTemplate mongoTemplate;

    /** Tope de reseñas por producto, para que el documento no crezca sin control. */
    @Value("${app.catalogo.max-comentarios:50}")
    private int maxComentarios;

    /**
     * Crea la reseña y la añade al array del producto.
     *
     * @param request  producto, texto y valoración.
     * @param username username del cliente.
     * @return la reseña creada.
     * @throws ResourceNotFoundException si el usuario, el producto o la ficha no existen.
     * @throws BusinessRuleException     si el usuario no tiene perfil de cliente o el producto alcanzó el tope de reseñas.
     */
    @Override
    @Transactional
    public ComentarioResponse crearComentario(ComentarioRequest request, String username) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Cliente cliente = clienteRepository.findByPersonaIdAndActivoTrue(usuario.getPersona().getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "Solo los clientes pueden comentar productos."));

        ProductoInventario producto = productoInventarioRepository
                .findByIdAndActivoTrue(request.getProductoInventarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado en inventario"));

        ProductoDocumento documento = productoMongoRepository.findByEan(producto.getEan())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado en catálogo (MongoDB)"));

        int comentariosActuales = documento.getComentarios() == null ? 0 : documento.getComentarios().size();
        if (comentariosActuales >= maxComentarios) {
            throw new BusinessRuleException("Este producto ha alcanzado el límite máximo de reseñas");
        }

        Comentario comentario = Comentario.builder()
                .id(UUID.randomUUID().toString())
                .clienteId(cliente.getId())
                .nombreCliente(nombreCompleto(usuario))
                .texto(request.getTexto().trim())
                .valoracion(request.getValoracion())
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .build();

        // $push atómico: no se reescribe el array, así que dos comentarios
        // simultáneos no se pisan.
        UpdateResult resultado = mongoTemplate.updateFirst(
                Query.query(Criteria.where("ean").is(producto.getEan())),
                new Update().push("comentarios", comentario),
                ProductoDocumento.class);

        if (resultado.getMatchedCount() == 0) {
            throw new ResourceNotFoundException("Producto no encontrado en catálogo (MongoDB)");
        }

        log.info("Reseña añadida al EAN {} por '{}'", producto.getEan(), username);

        return comentarioMapper.toResponse(comentario);
    }

    /**
     * Añade la respuesta oficial a una reseña existente.
     *
     * @param comentarioId UUID de la reseña.
     * @param request      texto de la respuesta.
     * @param username     username del staff.
     * @return la reseña actualizada.
     * @throws ResourceNotFoundException si la reseña no existe.
     */
    @Override
    @Transactional
    public ComentarioResponse responderComentario(String comentarioId,
                                                  RespuestaComentarioRequest request,
                                                  String username) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        ProductoDocumento documento = productoMongoRepository.findByComentariosId(comentarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado"));

        List<Comentario> comentarios = documento.getComentarios() != null
                ? documento.getComentarios()
                : List.of();

        Comentario comentario = comentarios.stream()
                .filter(c -> comentarioId.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado en la ficha"));

        String respuesta = request.getRespuestaOficial().trim();
        String nombreRespondedor = nombreCompleto(usuario);
        LocalDateTime fechaRespuesta = LocalDateTime.now();

        // $set posicional sobre el comentario concreto; el filtro de la query
        // localiza el documento y el arrayFilter el elemento.
        Update update = new Update()
                .set("comentarios.$[c].respuestaOficial", respuesta)
                .set("comentarios.$[c].nombreRespondedor", nombreRespondedor)
                .set("comentarios.$[c].usernameRespondedor", usuario.getUsername())
                .set("comentarios.$[c].fechaRespuesta", fechaRespuesta)
                .filterArray(Criteria.where("c._id").is(comentarioId));

        UpdateResult resultado = mongoTemplate.updateFirst(
                Query.query(Criteria.where("comentarios._id").is(comentarioId)),
                update,
                ProductoDocumento.class);

        if (resultado.getMatchedCount() == 0) {
            throw new ResourceNotFoundException("Comentario no encontrado");
        }

        // Refleja en la respuesta lo que se acaba de escribir, sin releer.
        comentario.setRespuestaOficial(respuesta);
        comentario.setNombreRespondedor(nombreRespondedor);
        comentario.setUsernameRespondedor(usuario.getUsername());
        comentario.setFechaRespuesta(fechaRespuesta);

        log.info("Respuesta oficial añadida a la reseña {} por '{}'", comentarioId, username);

        return comentarioMapper.toResponse(comentario);
    }

    /**
     * Reseñas activas de un producto.
     *
     * @param productoInventarioId id del producto en MySQL.
     * @return la lista de reseñas.
     * @throws ResourceNotFoundException si el producto o la ficha no existen.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ComentarioResponse> getComentariosByProducto(Long productoInventarioId) {
        ProductoInventario producto = productoInventarioRepository
                .findByIdAndActivoTrue(productoInventarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado en inventario"));

        ProductoDocumento documento = productoMongoRepository.findByEan(producto.getEan())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado en catálogo (MongoDB)"));

        List<Comentario> comentarios = documento.getComentarios() != null
                ? documento.getComentarios()
                : List.of();

        return comentarios.stream()
                .filter(comentario -> Boolean.TRUE.equals(comentario.getActivo()))
                .map(comentarioMapper::toResponse)
                .toList();
    }

    /**
     * Bandeja de reseñas sin respuesta. Resuelve el id de MySQL solo para los
     * EAN que aparecen en la bandeja.
     *
     * @return las reseñas pendientes con su producto.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ComentarioPendienteResponse> getComentariosPendientes() {
        List<ComentarioPendienteResponse> pendientes = productoMongoRepository.findComentariosPendientes();

        if (!pendientes.isEmpty()) {
            Set<String> eans = pendientes.stream()
                    .map(ComentarioPendienteResponse::getProductoEan)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<String, Long> idPorEan = productoInventarioRepository
                    .findAllByEanInAndActivoTrue(eans).stream()
                    .collect(Collectors.toMap(
                            ProductoInventario::getEan,
                            ProductoInventario::getId,
                            (primero, duplicado) -> primero));

            pendientes.forEach(pendiente ->
                    pendiente.setProductoId(idPorEan.get(pendiente.getProductoEan())));
        }

        return pendientes;
    }

    /**
     * Nombre y apellido del usuario, como snapshot para la reseña.
     *
     * @param usuario usuario autenticado.
     * @return el nombre completo.
     */
    private String nombreCompleto(Usuario usuario) {
        return usuario.getPersona().getNombre() + " " + usuario.getPersona().getApellido();
    }
}
