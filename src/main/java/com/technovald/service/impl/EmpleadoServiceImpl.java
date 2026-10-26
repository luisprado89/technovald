package com.technovald.service.impl;

import com.technovald.dto.request.EmpleadoRegistroRequest;
import com.technovald.dto.request.EmpleadoUpdateRequest;
import com.technovald.dto.response.EmpleadoRegistroResponse;
import com.technovald.dto.response.EmpleadoResponse;
import com.technovald.entity.Empleado;
import com.technovald.entity.Persona;
import com.technovald.entity.Usuario;
import com.technovald.enums.RolUsuario;
import com.technovald.exception.BusinessRuleException;
import com.technovald.exception.ResourceNotFoundException;
import com.technovald.mapper.EmpleadoMapper;
import com.technovald.mapper.PersonaMapper;
import com.technovald.repository.mysql.EmpleadoRepository;
import com.technovald.repository.mysql.PersonaRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.security.CredentialGenerator;
import com.technovald.service.EmpleadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Implementación de {@link EmpleadoService}.
 *
 * <p>
 * El alta es transaccional: persona, usuario y perfil de empleado se guardan en
 * la misma transacción, así que un fallo a mitad no deja un empleado a medias.
 * La generación de credenciales se delega en {@link CredentialGenerator} y el
 * hash en el {@link PasswordEncoder}: la contraseña en claro no se persiste ni
 * se escribe en el log.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmpleadoServiceImpl implements EmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;

    private final PersonaMapper personaMapper;
    private final EmpleadoMapper empleadoMapper;
    private final CredentialGenerator credentialGenerator;
    private final PasswordEncoder passwordEncoder;

    /**
     * Da de alta un empleado con el rol recibido ({@code ADMIN} o
     * {@code TRABAJADOR}) y devuelve sus credenciales generadas.
     *
     * <p>Pasos: normalizar → validar rol → comprobar duplicados → persistir
     * persona → generar credenciales → persistir usuario y perfil.</p>
     *
     * @throws BusinessRuleException si el rol no es válido, si el DNI o el email ya existen, o si no se consigue un
     *                               username libre.
     */
    @Override
    @Transactional
    public EmpleadoRegistroResponse createEmpleado(EmpleadoRegistroRequest empleadoRegistroRequest) {

        String dni = empleadoRegistroRequest.getDni().trim().toUpperCase(Locale.ROOT);
        String email = empleadoRegistroRequest.getEmail().trim().toLowerCase(Locale.ROOT);
        /*
         * El rol se valida aquí y no con una anotación porque la regla es de
         * negocio: CLIENTE es un valor válido del enum, pero nunca un rol de
         * empleado. La comparación doble también descarta un null.
         */
        RolUsuario rol = empleadoRegistroRequest.getRol();
        if (rol != RolUsuario.ADMIN && rol != RolUsuario.TRABAJADOR) {
            throw new BusinessRuleException("El empleado solo puede tener los roles ADMIN o TRABAJADOR");
        }
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

        Persona persona = personaMapper.toEntity(empleadoRegistroRequest);
        persona.setDni(dni);
        persona.setEmail(email);
        personaRepository.save(persona);

        String username = credentialGenerator.generateUsername(persona.getNombre(), persona.getApellido(),
                usuarioRepository::existsByUsername);
        String passwordInicial = credentialGenerator.generatePassword();

        Usuario usuario = Usuario.builder().username(username).password(passwordEncoder.encode(passwordInicial))
                .rol(rol).persona(persona).activo(true).tokenVersion(0).build();
        usuarioRepository.save(usuario);
        /*
         * Se enlaza también el lado inverso para que la persona recién creada
         * quede coherente en memoria (mismo criterio que el alta de clientes y
         * el bootstrap del administrador). El FK vive en la tabla usuario, que
         * ya está guardada.
         */
        persona.setUsuario(usuario);

        Empleado empleado = Empleado.builder().persona(persona).rol(rol).activo(true).build();
        empleadoRepository.save(empleado);

        log.info("Empleado dado de alta: username '{}' (empleado id {}, rol {})", username, empleado.getId(), rol);

        return EmpleadoRegistroResponse.builder().id(empleado.getId()).nombre(persona.getNombre())
                .apellido(persona.getApellido()).username(username)
                .passwordInicial(passwordInicial).rol(rol.name()).build();
    }

    /**
     * Devuelve el directorio de empleados en orden de alta.
     *
     * @return todos los empleados convertidos a DTO.
     */
    @Override
    @Transactional(readOnly = true)
    public List<EmpleadoResponse> getAllEmpleados() {
        return empleadoMapper.toResponseList(empleadoRepository.findAllByOrderByIdAsc());
    }

    /**
     * Obtiene un empleado activo por su id.
     *
     * @param id identificador del perfil de empleado.
     * @return el empleado convertido a DTO.
     * @throws ResourceNotFoundException si no existe o está inactivo.
     */
    @Override
    @Transactional(readOnly = true)
    public EmpleadoResponse getEmpleadoById(Long id) {
        Empleado empleado = empleadoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));
        return empleadoMapper.toResponse(empleado);
    }

    /**
     * Actualiza nombre, apellido, email, teléfono, dirección y rol.
     *
     * <p>El DNI y el username son inmutables. El rol se actualiza en las tablas
     * {@code usuario} y {@code empleado} para que no diverjan.</p>
     */
    @Override
    @Transactional
    public EmpleadoResponse updateEmpleado(Long id, EmpleadoUpdateRequest empleadoUpdateRequest) {
        Empleado empleado = empleadoPorId(id);
        Persona persona = empleado.getPersona();
        Usuario usuario = persona.getUsuario();

        RolUsuario rol = validarRol(empleadoUpdateRequest.getRol());
        String email = empleadoUpdateRequest.getEmail().trim().toLowerCase(Locale.ROOT);

        if (!email.equals(persona.getEmail()) && personaRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Ya existe otra persona registrada con el email indicado");
        }

        persona.setNombre(empleadoUpdateRequest.getNombre().trim());
        persona.setApellido(empleadoUpdateRequest.getApellido().trim());
        persona.setEmail(email);
        persona.setTelefono(empleadoUpdateRequest.getTelefono().trim());
        persona.setDireccion(empleadoUpdateRequest.getDireccion().trim());
        personaRepository.save(persona);

        empleado.setRol(rol);
        empleadoRepository.save(empleado);

        if (usuario != null) {
            usuario.setRol(rol);
            usuarioRepository.save(usuario);
        }

        log.info("Empleado actualizado: id {} (rol {})", id, rol);
        return empleadoMapper.toResponse(empleado);
    }

    /**
     * Baja lógica: desactiva el perfil y el usuario, impidiendo su login.
     */
    @Override
    @Transactional
    public void desactivarEmpleado(Long id) {
        Empleado empleado = empleadoPorId(id);
        empleado.setActivo(false);
        empleadoRepository.save(empleado);
        cambiarActivoUsuario(empleado, false);
        log.info("Empleado desactivado: id {}", id);
    }

    /**
     * Reactiva el perfil y el usuario, permitiendo de nuevo su login.
     */
    @Override
    @Transactional
    public void reactivarEmpleado(Long id) {
        Empleado empleado = empleadoPorId(id);
        empleado.setActivo(true);
        empleadoRepository.save(empleado);
        cambiarActivoUsuario(empleado, true);
        log.info("Empleado reactivado: id {}", id);
    }

    /**
     * Genera una contraseña temporal, guarda su hash e invalida los tokens previos.
     * Devuelve la contraseña en claro para que el ADMIN la entregue.
     */
    @Override
    @Transactional
    public EmpleadoRegistroResponse resetPassword(Long id) {
        Empleado empleado = empleadoPorId(id);
        Persona persona = empleado.getPersona();
        Usuario usuario = persona.getUsuario();

        if (usuario == null) {
            throw new BusinessRuleException("El empleado no tiene un usuario de acceso asociado");
        }

        String passwordTemporal = credentialGenerator.generatePassword();
        usuario.setPassword(passwordEncoder.encode(passwordTemporal));
        usuario.invalidateTokens();
        usuarioRepository.save(usuario);

        log.info("Contraseña reseteada para el empleado '{}' (id {})", usuario.getUsername(), id);

        return EmpleadoRegistroResponse.builder()
                .id(empleado.getId())
                .nombre(persona.getNombre())
                .apellido(persona.getApellido())
                .username(usuario.getUsername())
                .passwordInicial(passwordTemporal)
                .rol(empleado.getRol().name())
                .build();
    }

    /**
     * Busca un empleado por ID sin filtrar por su estado (activo/inactivo).
     */
    private Empleado empleadoPorId(Long id) {
        return empleadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado con id: " + id));
    }

    /**
     * Válida que el rol sea ADMIN o TRABAJADOR. CLIENTE es un valor válido del
     * enum, pero nunca un rol de empleado.
     */
    private RolUsuario validarRol(RolUsuario rol) {
        if (rol != RolUsuario.ADMIN && rol != RolUsuario.TRABAJADOR) {
            throw new BusinessRuleException("El empleado solo puede tener los roles ADMIN o TRABAJADOR");
        }
        return rol;
    }

    /**
     * Sincroniza el estado activo/inactivo del usuario asociado al empleado.
     */
    private void cambiarActivoUsuario(Empleado empleado, boolean activo) {
        Usuario usuario = empleado.getPersona().getUsuario();
        if (usuario != null) {
            usuario.setActivo(activo);
            usuarioRepository.save(usuario);
        }
    }

}