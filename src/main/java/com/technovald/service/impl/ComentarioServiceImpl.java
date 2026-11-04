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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * cliente, producto); el {@code save} de Mongo es atómico por documento.
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

    /**
     * Crea la reseña y la añade al array del producto.
     *
     * @param request  producto, texto y valoración.
     * @param username username del cliente.
     * @return la reseña creada.
     * @throws ResourceNotFoundException si el usuario, el producto o la ficha no existen.
     * @throws BusinessRuleException     si el usuario no tiene perfil de cliente.
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

        Comentario comentario = Comentario.builder()
                .id(UUID.randomUUID().toString())
                .clienteId(cliente.getId())
                .nombreCliente(nombreCompleto(usuario))
                .texto(request.getTexto().trim())
                .valoracion(request.getValoracion())
                .fechaCreacion(LocalDateTime.now())
                .activo(true)
                .build();

        if (documento.getComentarios() == null) {
            documento.setComentarios(new ArrayList<>());
        }
        documento.getComentarios().add(comentario);
        productoMongoRepository.save(documento);

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

        comentario.setRespuestaOficial(request.getRespuestaOficial().trim());
        comentario.setNombreRespondedor(nombreCompleto(usuario));
        comentario.setUsernameRespondedor(usuario.getUsername());
        comentario.setFechaRespuesta(LocalDateTime.now());

        productoMongoRepository.save(documento);

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
