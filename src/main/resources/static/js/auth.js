import {
    api,
    ApiError,
    clearSession,
    getSession,
    HOME_POR_ROL,
    saveSession,
} from './api.js';

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => document.querySelectorAll(selector);

const aviso = $('#aviso');
let credencialesCreadas = null;

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

// Los paneles se identifican con data-panel; las pestañas usan el mismo valor.
function mostrarPanel(nombre) {
    $$('.tab').forEach((tab) => {
        const activo = tab.dataset.panel === nombre;
        tab.classList.toggle('is-active', activo);
        tab.setAttribute('aria-selected', String(activo));
    });
    $$('.panel').forEach((panel) => {
        panel.classList.toggle('is-active', panel.dataset.panel === nombre);
    });
    ocultarAviso();
    const primerCampo = document.querySelector('.panel.is-active input:not([disabled])');
    if (primerCampo) primerCampo.focus();
}

function pintarSesion() {
    const sesion = getSession();
    const banner = $('#banner-sesion');
    banner.hidden = !sesion;
    if (!sesion) return;
    banner.querySelector('[data-usuario]').textContent = sesion.username || '—';
    banner.querySelector('[data-rol]').textContent = sesion.rol || '—';
    $('#btn-panel').href = HOME_POR_ROL[sesion.rol] ?? 'index.html';
}

$$('.tab').forEach((tab) => {
    tab.addEventListener('click', () => mostrarPanel(tab.dataset.panel));
});

$('#btn-salir').addEventListener('click', () => {
    clearSession();
    pintarSesion();
    mostrarPanel('login');
    mostrarAviso('Sesión cerrada.', 'ok');
});

$('#reg-dni').addEventListener('input', (event) => {
    event.target.value = event.target.value.toUpperCase();
});

$('#form-login').addEventListener('submit', async (event) => {
    event.preventDefault();
    const formulario = event.currentTarget;
    if (!formulario.reportValidity()) return;

    const boton = formulario.querySelector('button[type="submit"]');
    const textoOriginal = boton.textContent;
    boton.disabled = true;
    boton.textContent = 'Entrando…';
    ocultarAviso();

    try {
        const datos = new FormData(formulario);
        const sesion = await api.login(datos.get('username').trim(), datos.get('password'));
        saveSession(sesion);
        pintarSesion();
        mostrarAviso(`Bienvenido, ${sesion.username}. Abriendo tu panel…`, 'ok');
        window.location.href = HOME_POR_ROL[sesion.rol] ?? 'index.html';
    } catch (error) {
        mostrarAviso(mensajeDe(error));
        boton.disabled = false;
        boton.textContent = textoOriginal;
    }
});

$('#form-registro').addEventListener('submit', async (event) => {
    event.preventDefault();
    const formulario = event.currentTarget;
    if (!formulario.reportValidity()) return;

    const boton = formulario.querySelector('button[type="submit"]');
    const textoOriginal = boton.textContent;
    boton.disabled = true;
    boton.textContent = 'Creando cuenta…';
    ocultarAviso();

    try {
        const datos = Object.fromEntries(new FormData(formulario));
        datos.dni = datos.dni.trim().toUpperCase();

        const creado = await api.registrarCliente(datos);
        credencialesCreadas = { username: creado.username, password: creado.passwordInicial };

        $('#exito-username').textContent = creado.username;
        $('#exito-password').textContent = creado.passwordInicial;
        $$('.panel').forEach((panel) => {
            panel.classList.toggle('is-active', panel.dataset.panel === 'exito');
        });
        formulario.reset();
    } catch (error) {
        mostrarAviso(mensajeDe(error));
    } finally {
        boton.disabled = false;
        boton.textContent = textoOriginal;
    }
});

$('#btn-ir-login').addEventListener('click', () => {
    mostrarPanel('login');
    if (!credencialesCreadas) return;
    $('#login-username').value = credencialesCreadas.username;
    $('#login-password').value = credencialesCreadas.password;
    $('#login-password').focus();
});

pintarSesion();

const parametros = new URLSearchParams(window.location.search);
if (parametros.get('sesion') === 'expirada') {
    mostrarAviso('Tu sesión ha caducado. Vuelve a entrar.', 'error');
    window.history.replaceState(null, '', 'index.html');
}