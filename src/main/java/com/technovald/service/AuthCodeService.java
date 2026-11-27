package com.technovald.service;

import com.technovald.entity.Usuario;
import com.technovald.enums.PropositoCodigo;

/** Emisión y consumo de los códigos de un solo uso. */
public interface AuthCodeService {

    /**
     * Emite un código nuevo e invalida los vigentes del mismo propósito.
     *
     * @param usuario usuario al que pertenece el código.
     * @param purpose propósito del código.
     * @return el código en claro, solo para enviarlo por correo.
     */
    String emitir(Usuario usuario, PropositoCodigo purpose);

    /**
     * Verifica y consume el código vigente.
     *
     * @param usuarioId id del usuario.
     * @param purpose   propósito esperado.
     * @param codigo    código en claro introducido por el usuario.
     * @throws com.technovald.exception.InvalidCodeException si no es válido, caducó o se agotó.
     */
    void verificarYConsumir(Long usuarioId, PropositoCodigo purpose, String codigo);
}
