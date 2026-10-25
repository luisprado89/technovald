import { api, ApiError, clearSession, getSession } from './api.js';

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => document.querySelectorAll(selector);

const aviso = $('#aviso');
const sesion = getSession();
let empleados = [];
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

function mostrarCredenciales(creado) {
    $('#empleado-username').textContent = creado.username;
    $('#empleado-password').textContent = creado.passwordInicial;
    $('#empleado-rol').textContent = creado.rol;
    $('#exito-empleado').hidden = false;
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
    if (ultimoFoco) ultimoFoco.focus();
}

function parrafo(texto, clase) {
    const p = document.createElement('p');
    p.className = clase;
    p.textContent = texto;
    return p;
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
        $('#modal-titulo').textContent = `${empleado.nombre} ${empleado.apellido}`;
        contenido.replaceChildren(construirDetalle(empleado));
    } catch (error) {
        contenido.replaceChildren(parrafo(mensajeDe(error), 'modal__error'));
    }
}

/* ---------- eventos ---------- */

$('#emp-dni').addEventListener('input', (event) => {
    event.target.value = event.target.value.toUpperCase();
});

$('#form-empleado').addEventListener('submit', async (event) => {
    event.preventDefault();
    const formulario = event.currentTarget;
    if (!formulario.reportValidity()) return;

    const boton = formulario.querySelector('button[type="submit"]');
    const textoOriginal = boton.textContent;
    boton.disabled = true;
    boton.textContent = 'Creando…';
    ocultarAviso();

    try {
        const datos = Object.fromEntries(new FormData(formulario));
        datos.dni = datos.dni.trim().toUpperCase();

        const creado = await api.crearEmpleado(datos);
        formulario.reset();
        mostrarCredenciales(creado);
        mostrarAviso(`Empleado creado: ${creado.username}`, 'ok');
        await cargarEmpleados();
    } catch (error) {
        mostrarAviso(mensajeDe(error));
    } finally {
        boton.disabled = false;
        boton.textContent = textoOriginal;
    }
});

$('#btn-recargar').addEventListener('click', () => {
    ocultarAviso();
    cargarEmpleados();
});

$('#btn-copiar').addEventListener('click', async () => {
    const texto = `Usuario: ${$('#empleado-username').textContent}\n`
        + `Contraseña: ${$('#empleado-password').textContent}`;
    try {
        await navigator.clipboard.writeText(texto);
        mostrarAviso('Credenciales copiadas al portapapeles.', 'ok');
    } catch {
        mostrarAviso('No se pudo copiar. Selecciona el texto a mano.', 'error');
    }
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