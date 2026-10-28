import { api, ApiError, clearSession, getSession } from './api.js';
import { initCambiarPassword } from './cambiar-password.js';

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => document.querySelectorAll(selector);

const aviso = $('#aviso');
const sesion = getSession();
let empleados = [];
let empleadoDetalle = null;
let empleadoEnEdicion = null;
let ultimoFoco = null;

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
}

function celda(fila, texto) {
    const td = document.createElement('td');
    td.textContent = texto;
    fila.appendChild(td);
    return td;
}

function badge(texto, clase) {
    const span = document.createElement('span');
    span.className = `badge ${clase}`.trim();
    span.textContent = texto;
    return span;
}

function parrafo(texto, clase) {
    const p = document.createElement('p');
    p.className = clase;
    p.textContent = texto;
    return p;
}

/* ---------- directorio ---------- */

function pintarTabla() {
    const cuerpo = $('#tabla-empleados');
    cuerpo.replaceChildren();

    $('#contador').textContent =
        `${empleados.length} empleado${empleados.length === 1 ? '' : 's'}`;

    if (empleados.length === 0) {
        const fila = document.createElement('tr');
        const td = celda(fila, 'Todavía no hay empleados.');
        td.colSpan = 7;
        td.className = 'muted';
        cuerpo.appendChild(fila);
        return;
    }

    empleados.forEach((emp) => {
        const fila = document.createElement('tr');
        fila.className = 'fila';
        fila.tabIndex = 0;
        fila.title = 'Ver detalle';
        fila.addEventListener('click', () => abrirDetalle(emp.id));
        fila.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                abrirDetalle(emp.id);
            }
        });

        celda(fila, emp.id);
        celda(fila, `${emp.nombre} ${emp.apellido}`);
        celda(fila, emp.email);
        celda(fila, emp.telefono);
        celda(fila, emp.username ?? '—');

        const rol = document.createElement('td');
        rol.appendChild(badge(emp.rol, emp.rol === 'ADMIN' ? 'badge--admin' : 'badge--trabajador'));
        fila.appendChild(rol);

        const estado = document.createElement('td');
        estado.appendChild(emp.activo
            ? badge('Activo', '')
            : badge('Inactivo', 'badge--off'));
        fila.appendChild(estado);

        cuerpo.appendChild(fila);
    });
}

async function cargarEmpleados() {
    try {
        empleados = await api.listarEmpleados();
        pintarTabla();
    } catch (error) {
        mostrarAviso(mensajeDe(error));
    }
}

/* ---------- credenciales ---------- */

function mostrarCredenciales(creado) {
    $('#empleado-username').textContent = creado.username;
    $('#empleado-password').textContent = creado.passwordInicial;
    $('#empleado-rol').textContent = creado.rol;
    $('#exito-empleado').hidden = false;
}

async function copiarCredenciales(username, password) {
    try {
        await navigator.clipboard.writeText(`Usuario: ${username}\nContraseña: ${password}`);
        mostrarAviso('Credenciales copiadas al portapapeles.', 'ok');
    } catch {
        mostrarAviso('No se pudo copiar. Selecciona el texto a mano.', 'error');
    }
}

/** Sustituye el detalle del modal por las credenciales del reset. */
function mostrarCredencialesEnModal(creado) {
    const titulo = document.createElement('h3');
    titulo.textContent = 'Nueva contraseña temporal';

    const credenciales = document.createElement('div');
    credenciales.className = 'credenciales';

    const filaUsuario = document.createElement('div');
    filaUsuario.className = 'credencial';
    const etiquetaUsuario = document.createElement('span');
    etiquetaUsuario.textContent = 'Usuario';
    const codeUsuario = document.createElement('code');
    codeUsuario.textContent = creado.username;
    filaUsuario.append(etiquetaUsuario, codeUsuario);

    const filaPassword = document.createElement('div');
    filaPassword.className = 'credencial';
    const etiquetaPassword = document.createElement('span');
    etiquetaPassword.textContent = 'Contraseña';
    const codePassword = document.createElement('code');
    codePassword.textContent = creado.passwordInicial;
    filaPassword.append(etiquetaPassword, codePassword);

    credenciales.append(filaUsuario, filaPassword);

    const hint = document.createElement('p');
    hint.className = 'hint';
    hint.textContent = 'Guárdala y entrégala en persona: no se vuelve a mostrar.';

    const acciones = document.createElement('div');
    acciones.className = 'acciones bloque-acciones';
    const boton = document.createElement('button');
    boton.type = 'button';
    boton.className = 'btn btn--ghost btn--sm';
    boton.textContent = 'Copiar credenciales';
    boton.addEventListener('click', () => copiarCredenciales(creado.username, creado.passwordInicial));
    acciones.appendChild(boton);

    $('#modal-contenido').replaceChildren(titulo, credenciales, hint, acciones);
}

/* ---------- modal de detalle ---------- */

function abrirModal() {
    ultimoFoco = document.activeElement;
    $('#modal-empleado').hidden = false;
    document.body.classList.add('sin-scroll');
    $('#modal-cerrar').focus();
}

function cerrarModal() {
    $('#modal-empleado').hidden = true;
    document.body.classList.remove('sin-scroll');
    empleadoDetalle = null;
    if (ultimoFoco) ultimoFoco.focus();
}

function filaDetalle(dl, etiqueta, valor) {
    const dt = document.createElement('dt');
    dt.textContent = etiqueta;
    const dd = document.createElement('dd');
    if (valor instanceof Node) {
        dd.appendChild(valor);
    } else {
        dd.textContent = valor ?? '—';
    }
    dl.append(dt, dd);
}

function construirDetalle(emp) {
    const dl = document.createElement('dl');
    dl.className = 'detalle';

    filaDetalle(dl, 'ID', emp.id);
    filaDetalle(dl, 'Nombre', emp.nombre);
    filaDetalle(dl, 'Apellidos', emp.apellido);
    filaDetalle(dl, 'Email', emp.email);
    filaDetalle(dl, 'Teléfono', emp.telefono);
    filaDetalle(dl, 'Dirección', emp.direccion);
    filaDetalle(dl, 'Usuario', emp.username);
    filaDetalle(dl, 'Rol',
        badge(emp.rol, emp.rol === 'ADMIN' ? 'badge--admin' : 'badge--trabajador'));
    filaDetalle(dl, 'Estado',
        emp.activo ? badge('Activo', '') : badge('Inactivo', 'badge--off'));

    return dl;
}

async function abrirDetalle(id) {
    const contenido = $('#modal-contenido');
    $('#modal-titulo').textContent = 'Detalle del empleado';
    contenido.replaceChildren(parrafo('Cargando detalle…', 'muted'));
    abrirModal();

    try {
        const empleado = await api.obtenerEmpleado(id);
        empleadoDetalle = empleado;
        $('#modal-titulo').textContent = `${empleado.nombre} ${empleado.apellido}`;
        $('#modal-estado').textContent = empleado.activo ? 'Desactivar' : 'Reactivar';
        contenido.replaceChildren(construirDetalle(empleado));
    } catch (error) {
        contenido.replaceChildren(parrafo(mensajeDe(error), 'modal__error'));
    }
}

/* ---------- modo edición ---------- */

function entrarEnEdicion(empleado) {
    empleadoEnEdicion = empleado.id;

    $('#form-empleado-titulo').textContent = 'Editar empleado';
    $('#form-empleado-lead').textContent =
        `Editando el empleado #${empleado.id}. El DNI y el usuario no se modifican.`;
    $('#btn-guardar-empleado').textContent = 'Guardar cambios';
    $('#btn-cancelar-edicion').hidden = false;

    $('#emp-nombre').value = empleado.nombre ?? '';
    $('#emp-apellido').value = empleado.apellido ?? '';
    $('#emp-email').value = empleado.email ?? '';
    $('#emp-telefono').value = empleado.telefono ?? '';
    $('#emp-direccion').value = empleado.direccion ?? '';
    $('#emp-rol').value = empleado.rol;

    // El backend no devuelve el DNI: no se puede precargar.
    $('#emp-dni').value = '';
    $('#emp-dni').placeholder = 'No editable';
    $('#emp-dni').disabled = true;

    $('#exito-empleado').hidden = true;
    cerrarModal();
    $('#card-alta').scrollIntoView({ behavior: 'smooth', block: 'start' });
    $('#emp-nombre').focus();
}

function salirDeEdicion() {
    empleadoEnEdicion = null;
    $('#form-empleado').reset();
    $('#form-empleado-titulo').textContent = 'Nuevo empleado';
    $('#form-empleado-lead').textContent = 'Las credenciales se generan al guardar.';
    $('#btn-guardar-empleado').textContent = 'Crear empleado';
    $('#btn-cancelar-edicion').hidden = true;
    $('#emp-dni').disabled = false;
    $('#emp-dni').placeholder = '87654321X';
}

/** Deshabilita el botón mientras corre la acción y centraliza el error. */
async function ejecutarConBoton(boton, texto, accion) {
    const original = boton.textContent;
    boton.disabled = true;
    boton.textContent = texto;
    try {
        await accion();
        return true;
    } catch (error) {
        mostrarAviso(mensajeDe(error));
        return false;
    } finally {
        boton.disabled = false;
        boton.textContent = original;
    }
}

/* ---------- eventos ---------- */

initCambiarPassword();

$('#emp-dni').addEventListener('input', (event) => {
    event.target.value = event.target.value.toUpperCase();
});

$('#form-empleado').addEventListener('submit', async (event) => {
    event.preventDefault();
    const formulario = event.currentTarget;
    if (!formulario.reportValidity()) return;

    const boton = formulario.querySelector('button[type="submit"]');
    boton.disabled = true;
    boton.textContent = empleadoEnEdicion ? 'Guardando…' : 'Creando…';
    ocultarAviso();

    try {
        const datos = Object.fromEntries(new FormData(formulario));

        if (empleadoEnEdicion) {
            const actualizado = await api.actualizarEmpleado(empleadoEnEdicion, {
                nombre: datos.nombre.trim(),
                apellido: datos.apellido.trim(),
                email: datos.email.trim(),
                telefono: datos.telefono.trim(),
                direccion: datos.direccion.trim(),
                rol: datos.rol,
            });
            salirDeEdicion();
            mostrarAviso(`Empleado actualizado: ${actualizado.username}`, 'ok');
        } else {
            datos.dni = datos.dni.trim().toUpperCase();
            const creado = await api.crearEmpleado(datos);
            formulario.reset();
            mostrarCredenciales(creado);
            mostrarAviso(`Empleado creado: ${creado.username}`, 'ok');
        }

        await cargarEmpleados();
    } catch (error) {
        mostrarAviso(mensajeDe(error));
    } finally {
        boton.disabled = false;
        boton.textContent = empleadoEnEdicion ? 'Guardar cambios' : 'Crear empleado';
    }
});

$('#btn-cancelar-edicion').addEventListener('click', () => {
    salirDeEdicion();
    ocultarAviso();
});

$('#btn-recargar').addEventListener('click', () => {
    ocultarAviso();
    cargarEmpleados();
});

$('#btn-copiar').addEventListener('click', () => {
    copiarCredenciales(
        $('#empleado-username').textContent,
        $('#empleado-password').textContent,
    );
});

$('#modal-editar').addEventListener('click', () => {
    if (empleadoDetalle) entrarEnEdicion(empleadoDetalle);
});

$('#modal-estado').addEventListener('click', async () => {
    if (!empleadoDetalle) return;

    const { id, username, activo } = empleadoDetalle;
    const boton = $('#modal-estado');

    if (activo) {
        const avisoPropio = username === sesion.username
            ? ' Es tu propia cuenta: perderás el acceso.'
            : ' No podrá iniciar sesión.';
        if (!window.confirm(`¿Desactivar a ${username}?${avisoPropio}`)) return;

        const ok = await ejecutarConBoton(boton, 'Desactivando…', () => api.desactivarEmpleado(id));
        if (!ok) return;
        mostrarAviso(`Empleado desactivado: ${username}`, 'ok');
    } else {
        const ok = await ejecutarConBoton(boton, 'Reactivando…', () => api.reactivarEmpleado(id));
        if (!ok) return;
        mostrarAviso(`Empleado reactivado: ${username}`, 'ok');
    }

    cerrarModal();
    await cargarEmpleados();
});

$('#modal-reset').addEventListener('click', async () => {
    if (!empleadoDetalle) return;

    const { id, username } = empleadoDetalle;
    const boton = $('#modal-reset');

    const ok = await ejecutarConBoton(boton, 'Generando…', async () => {
        const credenciales = await api.resetPasswordEmpleado(id);
        mostrarCredencialesEnModal(credenciales);
    });
    if (!ok) return;

    mostrarAviso(`Contraseña reseteada para ${username}.`, 'ok');
    await cargarEmpleados();
});

$('#modal-cerrar').addEventListener('click', cerrarModal);
$('#modal-volver').addEventListener('click', cerrarModal);

$('#modal-empleado').addEventListener('click', (event) => {
    if (event.target.dataset.cerrar !== undefined) cerrarModal();
});

document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && !$('#modal-empleado').hidden) cerrarModal();
});

$('#btn-salir').addEventListener('click', () => {
    clearSession();
    window.location.replace('index.html');
});

if (!sesion) {
    window.location.replace('index.html');
} else {
    pintarCabecera(sesion);

    // La API reserva /api/admin/empleados al rol ADMIN.
    if (sesion.rol !== 'ADMIN') {
        mostrarAviso(`Tu rol (${sesion.rol}) no puede gestionar empleados. Esta sección es solo para ADMIN.`);
        $('#card-alta').hidden = true;
        $('#card-directorio').hidden = true;
    } else {
        cargarEmpleados();
    }
}