package com.technovald.entity;

import com.technovald.enums.RolUsuario;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;

/**
 * Entidad que almacena las credenciales de autenticación de un usuario
 * en TechNovaLD.
 *
 * <p>
 * Usuario es la entidad que el sistema de seguridad Spring Security
 * consulta para validar el inicio de sesión. Contiene el username, la
 * contraseña (cifrada con BCrypt) y el rol que se incrustará en el token
 * JWT para autorización basada en roles (RBAC).
 * </p>
 *
 * <p>
 * Usuario es el dueño de la relación con {@link Persona}: el FK
 * {@code persona_id} vive en la tabla {@code usuario}. Esto significa
 * que una Persona puede existir sin un Usuario, pero un Usuario no puede
 * existir sin una Persona.
 * </p>
 *
 * <p>
 * El campo {@code rol} es la fuente canónica para RBAC. El filtro JWT
 * ({@code JwtAuthenticationFilter}) lee este campo para construir las
 * autoridades de Spring Security. En el perfil {@link Empleado} existe
 * un campo {@code rol} redundante que permite consultar el sub-rol del
 * empleado sin realizar un JOIN con la tabla {@code usuario}.
 * </p>
 *
 * <p>
 * El campo {@code fechaCreacion} es el único campo de auditoría en el
 * modelo de identidad. Permite saber cuándo se dio de alta una cuenta.
 * Se establece automáticamente mediante {@code @PrePersist}.
 * </p>
 *
 * <p>
 * Usuario no declara {@code CascadeType} en la relación con Persona.
 * La filosofía es que Persona tiene su propio ciclo de vida y no debe
 * eliminarse físicamente cuando se modifica o desactiva un Usuario.
 * </p>
 *
 * <p><b>Índice:</b> definido con {@code @Index} usando {@code name = "idx_usuario_activo"}
 * y {@code columnList = "activo"}, para optimizar búsquedas frecuentes por estado
 * (usuarios activos/inactivos).</p>
 *
 * <p><b>Nota:</b> Los índices aceleran las lecturas ({@code SELECT}) a cambio de
 * un leve costo en escrituras ({@code INSERT}/{@code UPDATE}/{@code DELETE}) y
 * espacio en disco. Solo deben aplicarse sobre columnas usadas frecuentemente
 * en {@code WHERE}, {@code JOIN} u {@code ORDER BY}.</p>
 *
 * @see jakarta.persistence.Index
 */

@Entity
@Table(name = "usuario", indexes = {
        @Index(name = "idx_usuario_activo", columnList = "activo")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Usuario {
    /**
     * Identificador único del usuario.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de usuario para iniciar sesión.
     *
     * <p>
     * Se genera automáticamente durante el proceso de registro a partir
     * del nombre y primer apellido de la persona (ej:
     * {@code luciano.garcia}). Si el username ya existe, se añade un
     * sufijo numérico ({@code luciano.garcia1}, {@code luciano.garcia2},
     * etc.).
     * </p>
     *
     * <p>
     * Restricción {@code unique = true} a nivel de columna para
     * garantizar que no existan dos usuarios con el mismo username.
     * </p>
     */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /**
     * Contraseña del usuario cifrada con BCrypt.
     *
     * <p>
     * <b>Nunca</b> se almacena la contraseña en texto plano. La contraseña
     * original generada durante el registro se devuelve únicamente en la
     * respuesta HTTP de creación y posteriormente se descarta.
     * </p>
     *
     * <p>
     * El hash BCrypt incluye automáticamente el salt y es resistente a
     * ataques de fuerza bruta mediante el factor de coste configurable
     * en {@code SecurityConfig}.
     * </p>
     */
    @Column(name = "password", nullable = false)
    private String password;
    /**
     * Rol del usuario en el sistema.
     *
     * <p>
     * Es la fuente canónica para el control de acceso basado en roles
     * (RBAC). El token JWT incluye este rol en sus claims y el filtro
     * {@code JwtAuthenticationFilter} lo utiliza para construir las
     * autoridades de Spring Security.
     * </p>
     *
     * <p>
     * Se persiste como texto literal en MySQL mediante
     * {@code @Enumerated(EnumType.STRING)}. Se define explícitamente como
     * {@code VARCHAR(50)} usando {@code columnDefinition} para evitar que
     * Hibernate 6+ intente alterar o recrear la columna como un tipo
     * {@code ENUM} nativo en cada arranque, garantizando mayor portabilidad
     * y permitiendo consultar usuarios por rol de forma legible desde SQL.
     * </p>
     */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(50)", nullable = false)
    private RolUsuario rol;

    /**
     * Persona asociada al usuario.
     *
     * <p>
     * Usuario es el dueño de la relación: el FK {@code persona_id}
     * vive en la tabla {@code usuario}. Una persona puede tener como
     * máximo un usuario.
     * </p>
     *
     * <p>
     * {@code FetchType.LAZY} para evitar cargar los datos personales
     * cuando solo se necesitan las credenciales.
     * </p>
     *
     * <p>
     * {@code nullable = false} y {@code unique = true}: todo usuario
     * debe tener una persona, y una persona solo puede tener un usuario.
     * </p>
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    private Persona persona;

    /**
     * Fecha en la que se creó la cuenta de usuario.
     *
     * <p>
     * Es el único campo de auditoría del modelo de identidad. Permite
     * saber cuándo se dio de alta una cuenta, información útil para
     * reportes y auditoría.
     * </p>
     *
     * <p>
     * Se establece automáticamente mediante {@code @PrePersist}, por lo
     * que no es necesario setearlo manualmente en el servicio. El flag
     * {@code updatable = false} garantiza que no pueda modificarse en
     * futuras actualizaciones.
     * </p>
     */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Indica si el usuario se encuentra activo y puede autenticarse.
     *
     * <p>
     * Se utiliza para el borrado lógico (soft delete). Cuando un usuario
     * se desactiva, el {@code CustomUserDetailsService} no lo encontrará
     * al buscar por {@code username AND activo = true}, por lo que no
     * podrá iniciar sesión aunque sus credenciales sean correctas.
     * </p>
     *
     * <p>
     * {@code @Builder.Default} es vital aquí: sin esta anotación,
     * Lombok Builder dejaría el campo en {@code null} en lugar de
     * {@code true}.
     * </p>
     */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    /**
     * Versión de los tokens JWT emitidos para este usuario.
     *
     * <p>
     * Los JWT son <i>stateless</i>: una vez emitidos, no se pueden revocar por sí
     * solos. Para poder invalidarlos cuando la contraseña cambia, cada token se
     * emite con un claim {@code ver} que contiene el valor de este campo
     * ({@code JwtService.generateToken}). Al validar cada petición, se compara el
     * claim {@code ver} del token con el valor actual en base de datos
     * ({@code JwtService.isTokenValid}): si no coinciden, el token se rechaza.
     * </p>
     *
     * <p>
     * <b>Cómo se incrementa:</b> el método {@link #invalidateTokens()} suma 1 a
     * este campo. Se invoca desde tres sitios:
     * </p>
     * <ul>
     *     <li>{@code AuthServiceImpl.changePassword} — el usuario cambia su
     *     contraseña manualmente.</li>
     *     <li>{@code AuthServiceImpl.forgotPassword} — el usuario resetea su
     *     contraseña por email.</li>
     *     <li>{@code EmpleadoServiceImpl.resetPassword} — un administrador
     *     resetea la contraseña de un empleado.</li>
     * </ul>
     *
     * <p>
     * <b>Efecto:</b> al incrementar este valor, todos los JWT emitidos
     * previamente quedan invalidados, porque su claim {@code ver} ya no coincide
     * con el valor actual en base de datos. Esto permite expulsar a un atacante
     * que hubiera robado un token, aunque el token no haya expirado todavía.
     * </p>
     *
     * <p>
     * {@code @Builder.Default} garantiza que los usuarios nuevos empiecen en 0.
     * </p>
     */
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private Integer tokenVersion = 0;

    /**
     * Invalida todos los tokens JWT emitidos hasta ahora para este usuario.
     *
     * <p>
     * <b>Cómo lo hace:</b> incrementa en 1 el campo {@link #tokenVersion}. Como
     * los JWT se emiten con el valor de {@code tokenVersion} en el claim
     * {@code ver}, al incrementarlo, todos los tokens previos quedan con un
     * {@code ver} desactualizado y serán rechazados por
     * {@code JwtService.isTokenValid}.
     * </p>
     *
     * <p>
     * <b>Cuándo se llama:</b>
     * </p>
     * <ul>
     *     <li>{@code AuthServiceImpl.changePassword} — cambio manual del
     *     usuario.</li>
     *     <li>{@code AuthServiceImpl.forgotPassword} — reseteo por email.</li>
     *     <li>{@code EmpleadoServiceImpl.resetPassword} — reseteo por un
     *     administrador.</li>
     * </ul>
     *
     * <p>
     * <b>Por qué es necesario:</b> los JWT son <i>stateless</i> y, sin este
     * mecanismo, un token robado seguiría siendo válido hasta su expiración
     * (24 horas por defecto). Al incrementar {@code tokenVersion}, el usuario
     * puede expulsar al atacante simplemente cambiando su contraseña.
     * </p>
     *
     * @see #tokenVersion
     * @see com.technovald.security.JwtService#isTokenValid(String, UserDetails)
     */
    public void invalidateTokens() {
        this.tokenVersion = (tokenVersion == null ? 0 : tokenVersion) + 1;
    }

    /**
     * Callback de JPA que se ejecuta automáticamente antes de persistir
     * la entidad por primera vez (INSERT).
     *
     * <p>
     * Establece la fecha de creación al momento actual. De esta forma
     * no es necesario preocuparse por setear este campo en la capa de
     * servicio.
     * </p>
     */
    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
