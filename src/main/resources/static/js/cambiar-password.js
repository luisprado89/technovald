import { api, ApiError, clearSession } from './api.js';

const $ = (selector) => document.querySelector(selector);

/**
 * Conecta el botón y el modal de cambio de contraseña.
 * La página debe incluir #btn-cambiar y #modal-password.
 */
export function initCambiarPassword() {
    const modal = $('#modal-password');
    const formulario = $('#form-password');
    if (!modal || !formulario) return;

    const aviso = $('#aviso-password');
    let ultimoFoco = null;

    const mostrar = (mensaje) => {
        aviso.textContent = mensaje;
        aviso.hidden = false;
    };

    const ocultar = () => {
        aviso.hidden = true;
        aviso.textContent = '';
    };

    const abrir = () => {
        ultimoFoco = document.activeElement;
        modal.hidden = false;
        document.body.classList.add('sin-scroll');
        ocultar();
        formulario.reset();
        $('#pass-actual').focus();
    };

    const cerrar = () => {
        modal.hidden = true;
        document.body.classList.remove('sin-scroll');
        formulario.reset();
        ocultar();
        if (ultimoFoco) ultimoFoco.focus();
    };

    $('#btn-cambiar').addEventListener('click', abrir);
    $('#modal-password-cerrar').addEventListener('click', cerrar);
    $('#modal-password-cancelar').addEventListener('click', cerrar);

    modal.addEventListener('click', (event) => {
        if (event.target.dataset.cerrarPassword !== undefined) cerrar();
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !modal.hidden) cerrar();
    });

    formulario.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!formulario.reportValidity()) return;

        const actual = $('#pass-actual').value;
        const nueva = $('#pass-nueva').value;
        const repetir = $('#pass-repetir').value;

        if (nueva !== repetir) {
            mostrar('Las contraseñas nuevas no coinciden.');
            return;
        }
        if (nueva === actual) {
            mostrar('La nueva contraseña debe ser distinta de la actual.');
            return;
        }

        const boton = formulario.querySelector('button[type="submit"]');
        const textoOriginal = boton.textContent;
        boton.disabled = true;
        boton.textContent = 'Guardando…';
        ocultar();

        try {
            await api.changePassword(actual, nueva);
            // El backend invalida los tokens: hay que volver a entrar.
            clearSession();
            window.location.replace('index.html?sesion=cambiada');
        } catch (error) {
            mostrar(error instanceof ApiError ? error.message : 'Ha ocurrido un error inesperado.');
            boton.disabled = false;
            boton.textContent = textoOriginal;
        }
    });
}