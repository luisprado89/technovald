package com.technovald.service.impl;

import com.technovald.dto.request.ClienteRegistroRequest;
import com.technovald.dto.request.ClienteUpdateRequest;
import com.technovald.dto.response.ClienteAdminResponse;
import com.technovald.dto.response.ClienteRegistroResponse;
import com.technovald.dto.response.ClienteResponse;
import com.technovald.entity.Cliente;
import com.technovald.entity.Persona;
import com.technovald.entity.Usuario;
import com.technovald.enums.RolUsuario;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.ClienteMapper;
import com.technovald.mapper.PersonaMapper;
import com.technovald.repository.mysql.ClienteRepository;
import com.technovald.repository.mysql.PersonaRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.security.CredentialGenerator;
import com.technovald.security.CustomUserDetailsService;
import com.technovald.security.JwtService;
import com.technovald.service.ClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Implementación de {@link ClienteService}.
 *
 * <p>
 * El alta es transaccional: persona, usuario y perfil de cliente se guardan en
 * la misma transacción, así que un fallo a mitad no deja un cliente a medias.
 * La generación de credenciales se delega en {@link CredentialGenerator} y el
 * hash en el {@link PasswordEncoder} de Spring Security: la contraseña en
 * claro no se persiste ni se escribe en el log.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;

    private final PersonaMapper personaMapper;
    private final ClienteMapper clienteMapper;
    private final CredentialGenerator credentialGenerator;
    private final PasswordEncoder passwordEncoder;

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;

    /**
     * <p>Pasos:</p>
     * <ol>
     *     <li>Normaliza DNI (mayúsculas) y email (minúsculas).</li>
     *     <li>Comprueba que ni el DNI ni el email estén ya registrados.</li>
     *     <li>Persiste la persona.</li>
     *     <li>Genera un username libre y una contraseña aleatoria.</li>
     *     <li>Persiste el usuario con rol {@code CLIENTE} y el hash BCrypt.</li>
     *     <li>Persiste el perfil de cliente.</li>
     *     <li>Emite un token JWT para el cliente recién creado.</li>
     * </ol>
     *
     * @throws BusinessRuleException si el DNI o el email ya existen, o si no se
     *                               consigue un username libre.
     */
    @Override
    @Transactional
    public ClienteRegistroResponse registerCliente(ClienteRegistroRequest clienteRegistroRequest) {

        String dni = clienteRegistroRequest.getDni().trim().toUpperCase(Locale.ROOT);
        String email = clienteRegistroRequest.getEmail().trim().toLowerCase(Locale.ROOT);

        /*
         * Comprobación previa para poder responder 400 con un mensaje de
         * negocio. La restricción UNIQUE de la tabla sigue siendo la garantía
         * real: la comprobación sola no cubre dos altas simultáneas.
         */
        if (personaRepository.existsByDni(dni)) {
            throw new BusinessRuleException("Ya existe una persona registrada con el DNI indicado");
        }
        if (personaRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Ya existe una persona registrada con el email indicado");
        }

        Persona persona = personaMapper.toEntity(clienteRegistroRequest);
        persona.setDni(dni);
        persona.setEmail(email);
        personaRepository.save(persona);

        String username = credentialGenerator.generateUsername(
                persona.getNombre(), persona.getApellido(), usuarioRepository::existsByUsername);
        String passwordInicial = credentialGenerator.generatePassword();

        Usuario usuario = Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(passwordInicial))
                .rol(RolUsuario.CLIENTE)
                .persona(persona)
                .activo(true)
                .tokenVersion(0)
                .build();
        usuarioRepository.save(usuario);

        /*
         * Se enlaza también el lado inverso para que la persona recién creada
         * quede coherente en memoria (mismo criterio que el bootstrap del
         * administrador). El FK vive en la tabla usuario, que ya está guardada.
         */
        persona.setUsuario(usuario);

        Cliente cliente = Cliente.builder()
                .persona(persona)
                .activo(true)
                .build();
        clienteRepository.save(cliente);

        log.info("Cliente registrado: username '{}' (cliente id {})", username, cliente.getId());

        /*
         * El token se emite por el mismo camino que el login (el
         * CustomUserDetailsService construye las autoridades), para que el rol
         * del claim y el de una sesión normal no puedan divergir. La version de
         * credenciales se lee de la fila recién insertada, en esta misma
         * transacción.
         */
        String token = jwtService.generateToken(customUserDetailsService.loadUserByUsername(username));

        return ClienteRegistroResponse.builder()
                .id(cliente.getId())
                .nombre(persona.getNombre())
                .apellido(persona.getApellido())
                .username(username)
                .passwordInicial(passwordInicial)
                .rol(usuario.getRol().name())
                .token(token)
                .build();
    }

    /**
     * Obtiene los datos del cliente actualmente autenticado.
     *
     * @param username username del cliente extraído del JWT.
     * @return DTO con la información del perfil del cliente.
     * @throws ResourceNotFoundException si el usuario o el cliente no existen o están inactivos.
     */
    @Override
    @Transactional(readOnly = true)
    public ClienteResponse getMyCliente(String username) {
        Cliente cliente = obtenerClientePorUsername(username);
        return clienteMapper.toResponse(cliente);
    }

    /**
     * <p>Pasos:</p>
     * <ol>
     *     <li>Resuelve el cliente autenticado igual que la consulta.</li>
     *     <li>Normaliza el email nuevo (minúsculas).</li>
     *     <li>Comprueba que el email no pertenezca ya a otra persona.</li>
     *     <li>Escribe los tres campos de contacto en la persona asociada.</li>
     * </ol>
     *
     * <p>
     * El nombre, el apellido y el DNI no se tocan: no están en el DTO de
     * entrada, así que no hay forma de cambiarlos por esta vía.
     * </p>
     *
     * @throws BusinessRuleException si el email nuevo ya es de otra persona.
     */
    @Override
    @Transactional
    public ClienteResponse updateMyCliente(String username, ClienteUpdateRequest clienteUpdateRequest) {
        Cliente cliente = obtenerClientePorUsername(username);
        Persona persona = cliente.getPersona();
        String email = clienteUpdateRequest.getEmail().trim().toLowerCase(Locale.ROOT);
        /*
         * El email es único en toda la tabla persona. Solo se comprueba si
         * cambia: reenviar el mismo email no puede chocar consigo mismo. La
         * garantía real sigue siendo la clave uk_persona_email.
         */
        if (!email.equals(persona.getEmail()) && personaRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Ya existe otra persona registrada con el email indicado");
        }
        persona.setEmail(email);
        persona.setTelefono(clienteUpdateRequest.getTelefono().trim());
        persona.setDireccion(clienteUpdateRequest.getDireccion().trim());
        personaRepository.save(persona);
        log.info("Datos de contacto actualizados por el cliente '{}' (cliente id {})",
                username, cliente.getId());
        return clienteMapper.toResponse(cliente);
    }

    /**
     * Obtiene el perfil del cliente activo a partir del username del usuario autenticado.
     *
     * <p>Se usa para resolver el cliente en los endpoints /me. Si el usuario
     * autenticado no es un cliente (ej. es un ADMIN), lanza una excepción 404.</p>
     *
     * @param username username del usuario autenticado (extraído del token JWT).
     * @return la entidad Cliente.
     * @throws ResourceNotFoundException si el usuario no existe, está inactivo, o no tiene un perfil de cliente activo.
     */
    private Cliente obtenerClientePorUsername(String username) {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado o inactivo: " + username));
        return clienteRepository.findByPersonaIdAndActivoTrue(usuario.getPersona().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario autenticado no tiene un perfil de cliente activo"));
    }

    /**
     * Devuelve el directorio paginado de clientes (activos e inactivos) para el staff.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ClienteAdminResponse> listarClientes(Pageable pageable) {
        return clienteRepository.findAllByOrderByIdAsc(pageable)
                .map(clienteMapper::toAdminResponse);
    }

    /**
     * Obtiene la ficha de un cliente por su ID (sin filtrar por estado).
     */
    @Override
    @Transactional(readOnly = true)
    public ClienteAdminResponse obtenerCliente(Long id) {
        return clienteMapper.toAdminResponse(clientePorId(id));
    }

    /**
     * Actualiza los datos de contacto de un cliente desde el panel del staff.
     * El nombre, apellidos y DNI son inmutables.
     */
    @Override
    @Transactional
    public ClienteAdminResponse actualizarCliente(Long id, ClienteUpdateRequest request) {
        Cliente cliente = clientePorId(id);
        Persona persona = cliente.getPersona();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (!email.equals(persona.getEmail()) && personaRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Ya existe otra persona registrada con el email indicado");
        }

        persona.setEmail(email);
        persona.setTelefono(request.getTelefono().trim());
        persona.setDireccion(request.getDireccion().trim());
        personaRepository.save(persona);

        log.info("Datos de contacto actualizados por el staff (cliente id {})", id);
        return clienteMapper.toAdminResponse(cliente);
    }

    /**
     * Baja lógica: desactiva el cliente, la persona y el usuario asociado.
     */
    @Override
    @Transactional
    public void desactivarCliente(Long id) {
        cambiarEstadoCliente(clientePorId(id), false);
        log.info("Cliente desactivado: id {}", id);
    }

    /**
     * Reactiva el cliente, la persona y el usuario asociado.
     */
    @Override
    @Transactional
    public void reactivarCliente(Long id) {
        cambiarEstadoCliente(clientePorId(id), true);
        log.info("Cliente reactivado: id {}", id);
    }

    /**
     * Busca un cliente por ID sin filtrar por su estado.
     */
    private Cliente clientePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    /**
     * Sincroniza el estado activo/inactivo en las tres tablas vinculadas
     * (cliente, persona y usuario) para evitar incoherencias.
     */
    private void cambiarEstadoCliente(Cliente cliente, boolean activo) {
        cliente.setActivo(activo);
        clienteRepository.save(cliente);

        Persona persona = cliente.getPersona();
        persona.setActivo(activo);
        personaRepository.save(persona);

        Usuario usuario = persona.getUsuario();
        if (usuario != null) {
            usuario.setActivo(activo);
            usuarioRepository.save(usuario);
        }
    }
}