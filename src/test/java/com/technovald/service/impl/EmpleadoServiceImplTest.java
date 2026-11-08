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
import com.technovald.mapper.EmpleadoMapper;
import com.technovald.mapper.PersonaMapper;
import com.technovald.repository.mysql.EmpleadoRepository;
import com.technovald.repository.mysql.PersonaRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.security.CredentialGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpleadoServiceImplTest {

    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PersonaMapper personaMapper;
    @Mock private EmpleadoMapper empleadoMapper;
    @Mock private CredentialGenerator credentialGenerator;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private EmpleadoServiceImpl empleadoService;

    private Persona persona;
    private Usuario usuario;
    private Empleado empleado;

    @BeforeEach
    void setUp() {
        persona = Persona.builder()
                .id(1L)
                .nombre("Luis")
                .apellido("Pino")
                .dni("87654321X")
                .email("luis@test.local")
                .telefono("600000003")
                .direccion("Calle Test 2")
                .activo(true)
                .build();
        usuario = Usuario.builder()
                .id(2L)
                .username("luis.pino")
                .password("oldHash")
                .rol(RolUsuario.TRABAJADOR)
                .persona(persona)
                .activo(true)
                .tokenVersion(0)
                .build();
        persona.setUsuario(usuario);
        empleado = Empleado.builder()
                .id(3L)
                .persona(persona)
                .rol(RolUsuario.TRABAJADOR)
                .activo(true)
                .build();
    }

    private EmpleadoRegistroRequest registroRequest(RolUsuario rol) {
        return EmpleadoRegistroRequest.builder()
                .nombre("Luis")
                .apellido("Pino")
                .dni(" 87654321x ")
                .email(" Luis@Test.Local ")
                .telefono("600000003")
                .direccion("Calle Test 2")
                .rol(rol)
                .build();
    }

    @Test
    void creaEmpleadoConCredencialesYRole() {
        EmpleadoRegistroRequest request = registroRequest(RolUsuario.TRABAJADOR);
        when(personaRepository.existsByDni("87654321X")).thenReturn(false);
        when(personaRepository.existsByEmail("luis@test.local")).thenReturn(false);
        when(personaMapper.toEntity(request)).thenReturn(persona);
        when(credentialGenerator.generateUsername(eq("Luis"), eq("Pino"), any())).thenReturn("luis.pino");
        when(credentialGenerator.generatePassword()).thenReturn("Tmp5678!");
        when(passwordEncoder.encode("Tmp5678!")).thenReturn("hash");

        EmpleadoRegistroResponse resultado = empleadoService.createEmpleado(request);

        assertEquals("luis.pino", resultado.getUsername());
        assertEquals("Tmp5678!", resultado.getPasswordInicial());
        assertEquals("TRABAJADOR", resultado.getRol());
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertEquals(RolUsuario.TRABAJADOR, usuarioCaptor.getValue().getRol());
        verify(empleadoRepository).save(any(Empleado.class));
    }

    @Test
    void rechazaRolClienteEnUnEmpleado() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> empleadoService.createEmpleado(registroRequest(RolUsuario.CLIENTE)));

        assertTrue(ex.getMessage().contains("ADMIN o TRABAJADOR"));
        verifyNoInteractions(personaRepository, personaMapper, usuarioRepository, empleadoRepository);
    }

    @Test
    void rechazaDniDuplicado() {
        when(personaRepository.existsByDni("87654321X")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> empleadoService.createEmpleado(registroRequest(RolUsuario.TRABAJADOR)));

        assertTrue(ex.getMessage().contains("DNI"));
        verifyNoInteractions(personaMapper, usuarioRepository, empleadoRepository);
    }

    @Test
    void listaEmpleados() {
        EmpleadoResponse respuesta = EmpleadoResponse.builder().id(3L).build();
        when(empleadoRepository.findAllByOrderByIdAsc()).thenReturn(List.of(empleado));
        when(empleadoMapper.toResponseList(List.of(empleado))).thenReturn(List.of(respuesta));

        List<EmpleadoResponse> resultado = empleadoService.getAllEmpleados();

        assertEquals(1, resultado.size());
        assertSame(respuesta, resultado.get(0));
    }

    @Test
    void rechazaActualizacionConEmailDeOtraPersona() {
        EmpleadoUpdateRequest request = EmpleadoUpdateRequest.builder()
                .nombre("Luis").apellido("Pino").email("otro@test.local")
                .telefono("600000003").direccion("Calle Test 2").rol(RolUsuario.TRABAJADOR)
                .build();
        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleado));
        when(personaRepository.existsByEmail("otro@test.local")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> empleadoService.updateEmpleado(3L, request));

        assertTrue(ex.getMessage().contains("email"));
        verify(personaRepository, never()).save(any(Persona.class));
    }

    @Test
    void reseteaPasswordEInvalidaTokens() {
        EmpleadoRegistroResponse respuesta = EmpleadoRegistroResponse.builder()
                .id(3L).passwordInicial("Nueva123!").build();
        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleado));
        when(credentialGenerator.generatePassword()).thenReturn("Nueva123!");
        when(passwordEncoder.encode("Nueva123!")).thenReturn("newHash");

        EmpleadoRegistroResponse resultado = empleadoService.resetPassword(3L);

        assertEquals("Nueva123!", resultado.getPasswordInicial());
        assertEquals("newHash", usuario.getPassword());
        assertEquals(1, usuario.getTokenVersion());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void desactivaEmpleadoYUsuario() {
        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleado));

        empleadoService.desactivarEmpleado(3L);

        assertFalse(empleado.getActivo());
        assertFalse(usuario.getActivo());
        verify(empleadoRepository).save(empleado);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void reactivaEmpleadoYUsuario() {
        empleado.setActivo(false);
        usuario.setActivo(false);
        when(empleadoRepository.findById(3L)).thenReturn(Optional.of(empleado));

        empleadoService.reactivarEmpleado(3L);

        assertTrue(empleado.getActivo());
        assertTrue(usuario.getActivo());
        verify(empleadoRepository).save(empleado);
        verify(usuarioRepository).save(usuario);
    }
}
