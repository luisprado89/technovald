package com.technovald.security;

import com.technovald.entity.Usuario;
import com.technovald.repository.mysql.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String RAW_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String SECRET = Base64.getEncoder()
            .encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));

    @Mock private UsuarioRepository usuarioRepository;

    private JwtService jwtService;
    private UserDetails userDetails;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(usuarioRepository, SECRET, 3_600_000L);
        userDetails = User.withUsername("ana").password("hash").roles("ADMIN").build();
        usuario = Usuario.builder().username("ana").tokenVersion(0).build();
    }

    @Test
    void generaTokenConSujetoRolYVersionYLoValida() {
        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));

        String token = jwtService.generateToken(userDetails);

        assertEquals("ana", jwtService.extractUsername(token));
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        assertEquals("ADMIN", claims.get("rol"));
        assertEquals(0, ((Number) claims.get("ver")).intValue());
        assertTrue(jwtService.isTokenValid(token, userDetails));
        assertEquals(3600L, jwtService.getExpirationSeconds());
    }

    @Test
    void invalidaElTokenCuandoCambiaLaVersionDeCredenciales() {
        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));

        String token = jwtService.generateToken(userDetails);
        usuario.setTokenVersion(1);

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void invalidaElTokenDeOtroUsuario() {
        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));
        String token = jwtService.generateToken(userDetails);
        UserDetails otro = User.withUsername("otro").password("hash").roles("CLIENTE").build();

        assertFalse(jwtService.isTokenValid(token, otro));
    }

    @Test
    void devuelveFalseParaUnTokenMalFormado() {
        assertFalse(jwtService.isTokenValid("esto-no-es-un-jwt", userDetails));
    }

    @Test
    void noGeneraTokenSiElUsuarioYaNoExiste() {
        UserDetails fantasma = User.withUsername("fantasma").password("hash").roles("CLIENTE").build();
        when(usuarioRepository.findByUsernameAndActivoTrue("fantasma")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> jwtService.generateToken(fantasma));
    }

    @Test
    void rechazaUnTokenCaducado() throws InterruptedException {
        JwtService corto = new JwtService(usuarioRepository, SECRET, 1L);
        when(usuarioRepository.findByUsernameAndActivoTrue("ana")).thenReturn(Optional.of(usuario));

        String token = corto.generateToken(userDetails);
        Thread.sleep(20);

        assertFalse(corto.isTokenValid(token, userDetails));
    }
}
