package com.technovald.security;

import com.technovald.repository.mysql.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Servicio encargado de gestionar los tokens JWT utilizados para la
 * autenticación de los usuarios de TechNovaLD.
 *
 * <p>Se encarga de generar tokens, extraer su información y comprobar que
 * siguen siendo válidos: firma, caducidad y versión de credenciales.</p>
 *
 * <p>La clave de firma se construye <b>una sola vez</b>, al arrancar. Si
 * {@code jwt.secret} no es Base64 o no llega a 256 bits, la aplicación no
 * arranca, en lugar de fallar en el primer inicio de sesión.</p>
 */
@Service
public class JwtService {

    /** Claim con el rol del usuario, en el mismo formato que devuelve el login. */
    private static final String CLAIM_ROL = "rol";

    /** Claim con la versión de credenciales vigente al emitir el token. */
    private static final String CLAIM_VERSION = "ver";

    /** Prefijo que Spring Security exige en las autoridades. */
    private static final String ROLE_PREFIX = "ROLE_";

    /** Repositorio de usuarios: resuelve la versión de credenciales vigente. */
    private final UsuarioRepository usuarioRepository;

    /** Clave criptográfica con la que se firman y se verifican los tokens. */
    private final SecretKey signingKey;

    /** Tiempo de validez de un token, en milisegundos. */
    private final long expiration;

    /**
     * Construye el servicio y deja lista la clave de firma.
     *
     * @param usuarioRepository repositorio de usuarios.
     * @param secret            clave en Base64 de al menos 32 bytes.
     * @param expiration        vida del token en milisegundos.
     * @throws IllegalArgumentException si la expiración no es positiva.
     */
    public JwtService(UsuarioRepository usuarioRepository,
                      @Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expiration) {

        if (expiration <= 0) {
            throw new IllegalArgumentException(
                    "jwt.expiration debe ser un número positivo de milisegundos; valor recibido: " + expiration);
        }

        this.usuarioRepository = usuarioRepository;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiration = expiration;
    }

    /**
     * Genera un token para un usuario autenticado.
     *
     * <p>El token contiene el rol, la versión de credenciales vigente
     * (claim {@code ver}), el nombre de usuario como sujeto, la fecha de
     * emisión y la de expiración. Se firma con HMAC-SHA usando la clave
     * configurada.</p>
     *
     * @param userDetails usuario autenticado.
     * @return el token firmado.
     * @throws UsernameNotFoundException si el usuario ya no existe o está inactivo.
     */
    public String generateToken(UserDetails userDetails) {
        Date ahora = new Date();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(CLAIM_ROL, rolDe(userDetails))
                .claim(CLAIM_VERSION, currentTokenVersion(userDetails.getUsername()))
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiration))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extrae el nombre de usuario almacenado como sujeto del token.
     *
     * <p>Se usa para localizar al usuario antes de poder validar el token:
     * la versión de credenciales vive en la base de datos, no en el token.</p>
     *
     * @param token token JWT.
     * @return el nombre de usuario del token.
     * @throws JwtException si el token está manipulado, caducado o mal formado.
     */
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    /**
     * Comprueba si un token es válido para un usuario.
     *
     * <p>Un token vale cuando su firma es correcta, el sujeto coincide con el
     * usuario, no ha caducado y su claim {@code ver} coincide con la versión de
     * credenciales vigente. Un token sin ese claim (emitido antes de existir la
     * revocación) se considera inválido.</p>
     *
     * <p><b>No lanza nunca</b>: un token ilegible es un token inválido, y la
     * respuesta que corresponde es 401, no un error del servidor.</p>
     *
     * @param token       token JWT.
     * @param userDetails usuario cargado desde la base de datos.
     * @return {@code true} solo si el token sirve para ese usuario.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = parse(token);

            if (!userDetails.getUsername().equals(claims.getSubject())) {
                return false;
            }

            if (!claims.getExpiration().after(new Date())) {
                return false;
            }

            /*
             * La versión se exige como número: así un token sin el claim no
             * puede colarse comparándose con la versión por defecto.
             */
            Object versionDelToken = claims.get(CLAIM_VERSION);
            if (!(versionDelToken instanceof Number numero)) {
                return false;
            }

            return numero.intValue() == currentTokenVersion(userDetails.getUsername());

        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            return false;
        }
    }

    /**
     * Obtiene el rol del usuario en el formato que usa el resto de la API
     * ({@code ADMIN}, no {@code ROLE_ADMIN}).
     */
    private String rolDe(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(autoridad -> autoridad.startsWith(ROLE_PREFIX)
                        ? autoridad.substring(ROLE_PREFIX.length())
                        : autoridad)
                .orElse("");
    }

    /**
     * Versión de credenciales vigente del usuario.
     *
     * <p>Cuesta una consulta por petición autenticada: es el precio de poder
     * revocar tokens en una arquitectura sin estado.</p>
     *
     * @param username nombre de usuario.
     * @return la versión almacenada, o {@code 0} si el campo es nulo.
     * @throws UsernameNotFoundException si el usuario no existe o está inactivo.
     */
    private int currentTokenVersion(String username) {
        return usuarioRepository.findByUsernameAndActivoTrue(username)
                .map(usuario -> usuario.getTokenVersion() == null ? 0 : usuario.getTokenVersion())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado o inactivo: " + username));
    }

    /** Verifica la firma del token y devuelve su contenido. */
    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Vida del token en segundos.
     *
     * <p>Se usa para que el login pueda devolver al cliente la fecha de
     * caducidad del token recién emitido, sin necesidad de decodificarlo.</p>
     *
     * @return la vida configurada del token, en segundos.
     */
    public long getExpirationSeconds() {
        return expiration / 1000;
    }
}