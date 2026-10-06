package com.technovald.seeder;

import com.technovald.entity.Empleado;
import com.technovald.entity.Persona;
import com.technovald.entity.Usuario;
import com.technovald.enums.RolUsuario;
import com.technovald.repository.mysql.EmpleadoRepository;
import com.technovald.repository.mysql.PersonaRepository;
import com.technovald.repository.mysql.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/*
 * NOTA: cuando se añada DataInitializer (fase de seeders) y
 * application-dev.properties (fase de entornos), esta clase tendrá que
 * llevar @Profile("!dev") y comprobar app.seed.enabled, como en el
 * proyecto terminado. Ahora mismo no existe ninguno de los dos, así que
 * se ejecuta siempre que la tabla usuario esté vacía.
 */

/**
 * Crea el administrador inicial la primera vez que arranca la aplicación.
 *
 * <p>Solo actúa si la tabla {@code usuario} está vacía, así que es idempotente
 * y nunca sobrescribe datos existentes. En arranques posteriores no hace nada y
 * ni siquiera hacen falta las credenciales de configuración.</p>
 *
 * <p>Las credenciales se leen de {@code app.bootstrap.admin-username} y
 * {@code app.bootstrap.admin-password} (variables de entorno
 * {@code BOOTSTRAP_ADMIN_USERNAME} y {@code BOOTSTRAP_ADMIN_PASSWORD}) y no
 * tienen valor por defecto: si la base de datos está vacía y faltan, la
 * aplicación falla al arrancar en lugar de crear un administrador con
 * credenciales conocidas. La contraseña nunca se escribe en el log.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-username:}")
    private String adminUsername;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        if (usuarioRepository.count() > 0) {
            log.info("La base de datos ya contiene usuarios. Omitiendo el bootstrap del administrador.");
            return;
        }

        /*
         * Fail-fast: una variable definida pero vacía no la detecta Spring, así
         * que se valida aquí de forma explícita.
         */
        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap de administrador: la tabla usuario está vacía y faltan las "
                            + "credenciales. Define BOOTSTRAP_ADMIN_USERNAME y "
                            + "BOOTSTRAP_ADMIN_PASSWORD antes de arrancar.");
        }

        log.info("=== BOOTSTRAP DE ADMINISTRADOR INICIAL ===");

        /*
         * La tabla persona exige nombre, apellido, dni, email, telefono y
         * direccion. Al no ser datos configurables se rellenan con valores
         * técnicos que el administrador puede cambiar al editar su perfil.
         */
        Persona personaAdmin = Persona.builder()
                .nombre("Admin")
                .apellido("TechNovaLD")
                .dni("00000000T")
                .email(adminUsername + "@technovald.local")
                .telefono("600000000")
                .direccion("Bootstrap")
                .activo(true)
                .build();
        personaRepository.save(personaAdmin);

        Usuario usuarioAdmin = Usuario.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .rol(RolUsuario.ADMIN)
                .persona(personaAdmin)
                .activo(true)
                .tokenVersion(0)
                .build();
        usuarioRepository.save(usuarioAdmin);

        /*
         * Ajuste respecto al plan inicial de la fase: se enlaza la persona con
         * su usuario recién creado y se guarda otra vez. Es lo que hace el
         * proyecto terminado y mantiene la relación bidireccional coherente.
         */
        personaAdmin.setUsuario(usuarioAdmin);
        personaRepository.save(personaAdmin);

        empleadoRepository.save(Empleado.builder()
                .persona(personaAdmin)
                .rol(RolUsuario.ADMIN)
                .activo(true)
                .build());

        log.info("Administrador inicial creado con username '{}'.", adminUsername);
    }
}