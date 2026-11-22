package com.technovald.service.impl;

import com.technovald.dto.request.BajaCuentaClienteRequest;
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
import com.technovald.mapper.ClienteMapper;
import com.technovald.mapper.PersonaMapper;
import com.technovald.repository.mysql.ClienteRepository;
import com.technovald.repository.mysql.PersonaRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import com.technovald.security.CredentialGenerator;
import com.technovald.security.CustomUserDetailsService;
import com.technovald.security.JwtService;
import com.technovald.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock private ClienteRepository clienteRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PersonaMapper personaMapper;
    @Mock private ClienteMapper clienteMapper;
    @Mock private CredentialGenerator credentialGenerator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private CustomUserDetailsService customUserDetailsService;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;

    @InjectMocks private ClienteServiceImpl clienteService;

    private Persona persona;
    private Usuario usuario;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        persona = Persona.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("García")
                .dni("12345678Z")
                .email("ana@test.local")
                .telefono("600000001")
                .direccion("Calle Test 1")
                .activo(true)
                .build();
        usuario = Usuario.builder()
                .id(2L)
                .username("ana.garcia")
                .password("oldHash")
                .rol(RolUsuario.CLIENTE)
                .persona(persona)
                .activo(true)
                .tokenVersion(0)
                .build();
        persona.setUsuario(usuario);
        cliente = Cliente.builder().id(5L).persona(persona).activo(true).build();
    }

    private ClienteRegistroRequest registroRequest() {
        return ClienteRegistroRequest.builder()
                .nombre("Ana")
                .apellido("García")
                .dni(" 12345678z ")
                .email(" Ana@Test.Local ")
                .telefono("600000001")
                .direccion("Calle Test 1")
                .build();
    }

    @Test
    void registraClienteConCredencialesYToken() {
        ClienteRegistroRequest request = registroRequest();
        UserDetails userDetails = User.withUsername("ana.garcia").password("hash").roles("CLIENTE").build();

        when(personaRepository.existsByDni("12345678Z")).thenReturn(false);
        when(personaRepository.existsByEmail("ana@test.local")).thenReturn(false);
        when(personaMapper.toEntity(request)).thenReturn(persona);
        when(credentialGenerator.generateUsername(eq("Ana"), eq("García"), any())).thenReturn("ana.garcia");
        when(credentialGenerator.generatePassword()).thenReturn("Tmp1234!");
        when(passwordEncoder.encode("Tmp1234!")).thenReturn("hash");
        when(customUserDetailsService.loadUserByUsername("ana.garcia")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        ClienteRegistroResponse resultado = clienteService.registerCliente(request);

        assertEquals("ana.garcia", resultado.getUsername());
        assertEquals("Tmp1234!", resultado.getPasswordInicial());
        assertEquals("CLIENTE", resultado.getRol());
        assertEquals("jwt-token", resultado.getToken());
        assertEquals("ana@test.local", persona.getEmail());
        assertEquals("12345678Z", persona.getDni());
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertEquals(RolUsuario.CLIENTE, usuarioCaptor.getValue().getRol());
        assertEquals("hash", usuarioCaptor.getValue().getPassword());
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    void rechazaDniDuplicado() {
        when(personaRepository.existsByDni("12345678Z")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> clienteService.registerCliente(registroRequest()));

        assertTrue(ex.getMessage().contains("DNI"));
        verifyNoInteractions(personaMapper, credentialGenerator, clienteRepository);
    }

    @Test
    void rechazaEmailDuplicado() {
        when(personaRepository.existsByDni("12345678Z")).thenReturn(false);
        when(personaRepository.existsByEmail("ana@test.local")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> clienteService.registerCliente(registroRequest()));

        assertTrue(ex.getMessage().contains("email"));
        verifyNoInteractions(personaMapper, credentialGenerator, clienteRepository);
    }

    @Test
    void obtieneElPerfilDelClienteAutenticado() {
        ClienteResponse respuesta = ClienteResponse.builder().id(5L).nombre("Ana").build();
        when(usuarioRepository.findByUsernameAndActivoTrue("ana.garcia")).thenReturn(Optional.of(usuario));
        when(clienteRepository.findByPersonaIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(clienteMapper.toResponse(cliente)).thenReturn(respuesta);

        ClienteResponse resultado = clienteService.getMyCliente("ana.garcia");

        assertSame(respuesta, resultado);
    }

    @Test
    void rechazaEmailDeOtroClienteEnLaActualizacion() {
        ClienteUpdateRequest request = ClienteUpdateRequest.builder()
                .email("nuevo@test.local").telefono("600000002").direccion("Calle 2").build();
        when(usuarioRepository.findByUsernameAndActivoTrue("ana.garcia")).thenReturn(Optional.of(usuario));
        when(clienteRepository.findByPersonaIdAndActivoTrue(1L)).thenReturn(Optional.of(cliente));
        when(personaRepository.existsByEmail("nuevo@test.local")).thenReturn(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> clienteService.updateMyCliente("ana.garcia", request));

        assertTrue(ex.getMessage().contains("email"));
        verify(personaRepository, never()).save(any(Persona.class));
    }

    @Test
    void listaClientesPaginados() {
        Pageable pageable = PageRequest.of(0, 10);
        ClienteAdminResponse respuesta = ClienteAdminResponse.builder().id(5L).build();
        Page<Cliente> page = new PageImpl<>(List.of(cliente));
        when(clienteRepository.findAllByOrderByIdAsc(pageable)).thenReturn(page);
        when(clienteMapper.toAdminResponse(cliente)).thenReturn(respuesta);

        Page<ClienteAdminResponse> resultado = clienteService.listarClientes(pageable);

        assertEquals(1, resultado.getTotalElements());
        assertSame(respuesta, resultado.getContent().get(0));
    }

    @Test
    void desactivaClientePersonaYUsuario() {
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));

        clienteService.desactivarCliente(5L);

        assertFalse(cliente.getActivo());
        assertFalse(persona.getActivo());
        assertFalse(usuario.getActivo());
        verify(clienteRepository).save(cliente);
        verify(personaRepository).save(persona);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void reactivaClientePersonaYUsuario() {
        cliente.setActivo(false);
        persona.setActivo(false);
        usuario.setActivo(false);
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));

        clienteService.reactivarCliente(5L);

        assertTrue(cliente.getActivo());
        assertTrue(persona.getActivo());
        assertTrue(usuario.getActivo());
        verify(clienteRepository).save(cliente);
        verify(personaRepository).save(persona);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void desactivaCuentaPropiaYNotificaElMotivo() {
        when(usuarioRepository.findByUsernameAndActivoTrue("ana.garcia"))
                .thenReturn(Optional.of(usuario));
        when(clienteRepository.findByPersonaIdAndActivoTrue(1L))
                .thenReturn(Optional.of(cliente));

        clienteService.desactivarCuentaPropiaCliente(
                "ana.garcia",
                new BajaCuentaClienteRequest("No me gusta la tienda"));

        assertFalse(cliente.getActivo());
        assertFalse(persona.getActivo());
        assertFalse(usuario.getActivo());
        verify(emailService).sendBajaCuentaEmail(
                "ana@test.local", "Ana", "No me gusta la tienda");
    }

    @Test
    void laBajaPropiaNoSeRevierteSiFallaElCorreo() {
        when(usuarioRepository.findByUsernameAndActivoTrue("ana.garcia"))
                .thenReturn(Optional.of(usuario));
        when(clienteRepository.findByPersonaIdAndActivoTrue(1L))
                .thenReturn(Optional.of(cliente));
        doThrow(new RuntimeException("SMTP caído"))
                .when(emailService).sendBajaCuentaEmail(any(), any(), any());

        assertDoesNotThrow(() -> clienteService.desactivarCuentaPropiaCliente(
                "ana.garcia",
                new BajaCuentaClienteRequest("Motivo de prueba")));

        assertFalse(cliente.getActivo());
        assertFalse(persona.getActivo());
        assertFalse(usuario.getActivo());
    }
}
