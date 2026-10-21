package com.technovald.service.impl;

import com.technovald.dto.request.EmpleadoRegistroRequest;
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
}