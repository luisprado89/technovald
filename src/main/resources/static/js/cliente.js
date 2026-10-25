import { api, ApiError, clearSession, getSession } from './api.js';
import { initCambiarPassword } from './cambiar-password.js';

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => document.querySelectorAll(selector);

const aviso = $('#aviso');
const sesion = getSession();
let perfilActual = null;

function mostrarAviso(mensaje, tipo = 'error') {
    aviso.textContent = mensaje;
    aviso.className = `aviso aviso--${tipo}`;
    aviso.hidden = false;
}

function ocultarAviso() {
    aviso.hidden = true;
    aviso.textContent = '';
}

function mensajeDe(error) {
    if (error instanceof ApiError) return error.message;
    console.error(error);
    return 'Ha ocurrido un error inesperado.';
}

function pintarCabecera(usuario) {
    $$('[data-usuario]').forEach((el) => { el.textContent = usuario.username; });
    $$('[data-rol]').forEach((el) => { el.textContent = usuario.rol; });
    $('#perfil-usuario').value = usuario.username;
}

function pintarPerfil(perfil) {
    perfilActual = perfil;

    $('#perfil-nombre').value = perfil.nombre ?? '';
    $('#perfil-apellido').value = perfil.apellido ?? '';
    $('#perfil-email').value = perfil.email ?? '';
    $('#perfil-telefono').value = perfil.telefono ?? '';
    $('#perfil-direccion').value = perfil.direccion ?? '';
    $('#perfil-id').textContent = `#${perfil.id}`;

    const estado = $('#perfil-estado');
    estado.textContent = perfil.activo ? 'Activo' : 'Inactivo';
    estado.className = perfil.activo ? 'badge' : 'badge badge--off';
}

async function cargarPerfil() {
    try {
        pintarPerfil(await api.miPerfil());
    } catch (error) {
        mostrarAviso(mensajeDe(error));
        $('#form-perfil').hidden = true;
    }
}

initCambiarPassword();

$('#form-perfil').addEventListener('submit', async (event) => {
    event.preventDefault();
    const formulario = event.currentTarget;
    if (!formulario.reportValidity()) return;

    const boton = formulario.querySelector('button[type="submit"]');
    const textoOriginal = boton.textContent;
    boton.disabled = true;
    boton.textContent = 'Guardando…';
    ocultarAviso();

    try {
        // La API solo acepta email, teléfono y dirección.
        const datos = new FormData(formulario);
        const actualizado = await api.actualizarPerfil({
            email: datos.get('email').trim(),
            telefono: datos.get('telefono').trim(),
            direccion: datos.get('direccion').trim(),
        });

        pintarPerfil(actualizado);
        mostrarAviso('Datos guardados.', 'ok');
    } catch (error) {
        mostrarAviso(mensajeDe(error));
    } finally {
        boton.disabled = false;
        boton.textContent = textoOriginal;
    }
});

$('#btn-descartar').addEventListener('click', () => {
    if (perfilActual) pintarPerfil(perfilActual);
    ocultarAviso();
});

$('#btn-salir').addEventListener('click', () => {
    clearSession();
    window.location.replace('index.html');
});

if (!sesion) {
    window.location.replace('index.html');
} else {
    pintarCabecera(sesion);

    if (sesion.rol !== 'CLIENTE') {
        mostrarAviso(`Esta página es para clientes. Tu rol es ${sesion.rol}.`);
        $('#form-perfil').hidden = true;
    } else {
        cargarPerfil();
    }
}