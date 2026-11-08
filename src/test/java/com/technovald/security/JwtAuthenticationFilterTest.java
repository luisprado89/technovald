package com.technovald.security;

import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest("GET", "/api/pedidos");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinCabeceraAuthorizationDejaLaPeticionSinAutenticar() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void autenticaUnaPeticionConTokenValido() throws Exception {
        UserDetails user = User.withUsername("ana").password("hash").roles("ADMIN").build();
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.extractUsername("token")).thenReturn("ana");
        when(userDetailsService.loadUserByUsername("ana")).thenReturn(user);
        when(jwtService.isTokenValid("token", user)).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertEquals("ana", SecurityContextHolder.getContext().getAuthentication().getName());
        assertSame(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void unTokenMalFormadoNoAutenticaNiRompeLaCadena() throws Exception {
        request.addHeader("Authorization", "Bearer bad");
        when(jwtService.extractUsername("bad")).thenThrow(new MalformedJwtException("bad token"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void unUsuarioInexistenteNoAutentica() throws Exception {
        request.addHeader("Authorization", "Bearer ghost");
        when(jwtService.extractUsername("ghost")).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("ghost"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void noSobrescribeUnaAutenticacionYaExistente() throws Exception {
        UsernamePasswordAuthenticationToken existente =
                new UsernamePasswordAuthenticationToken("test", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existente);
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.extractUsername("token")).thenReturn("ana");

        filter.doFilter(request, response, chain);

        assertSame(existente, SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void ignoraUnaCabeceraQueNoEsBearer() throws Exception {
        request.addHeader("Authorization", "Basic abc");

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userDetailsService);
    }
}
