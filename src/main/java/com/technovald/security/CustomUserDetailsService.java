package com.technovald.security;

import com.technovald.entity.Usuario;
import com.technovald.repository.mysql.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    /**
     * Localiza al usuario por su username.
     *
     * @param username nombre de usuario introducido en la autenticación..
     * @return {@link UserDetails} con las credenciales y autoridades.
     * @throws UsernameNotFoundException si el usuario no existe o está inactivo.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsernameAndActivoTrue(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado o inactivo: " + username));

        /*
         * Spring Security requiere que los roles lleven el prefijo "ROLE_".
         * Por ejemplo, si el rol en BD es "ADMIN", la autoridad será
         * "ROLE_ADMIN". Esto es vital para que funcione @PreAuthorize("hasRole('ADMIN')").
         */
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                "ROLE_" + usuario.getRol().name());

        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                Collections.singletonList(authority)
        );
    }
}
