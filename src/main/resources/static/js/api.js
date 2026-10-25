// El frontend lo sirve Spring en el mismo 8080, así que basta la ruta relativa.
// Si lo sirves desde otro puerto, cambia esto por 'http://localhost:8080/api'.
const API_BASE = '/api';

export const CLAVES = {
    token: 'tn.token',
    username: 'tn.username',
    rol: 'tn.rol',
};

export const HOME_POR_ROL = {
    ADMIN: 'admin.html',
    TRABAJADOR: 'admin.html',
    CLIENTE: 'cliente.html',
};

export class ApiError extends Error {
    constructor(status, message, body = null) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.body = body;
    }
}

export function saveSession({ token, username, rol }) {
    localStorage.setItem(CLAVES.token, token);
    localStorage.setItem(CLAVES.username, username);
    localStorage.setItem(CLAVES.rol, rol);
}

export function getSession() {
    const token = localStorage.getItem(CLAVES.token);
    if (!token) return null;
    return {
        token,
        username: localStorage.getItem(CLAVES.username) ?? '',
        rol: localStorage.getItem(CLAVES.rol) ?? '',
    };
}

export function clearSession() {
    Object.values(CLAVES).forEach((clave) => localStorage.removeItem(clave));
}

// El 401 y el 403 de la cadena de filtros llegan sin cuerpo: por eso hay
// mensajes de reserva cuando no hay JSON con 'message'.
function mensajeDeError(status, datos) {
    if (datos?.message) return datos.message;
    if (status === 401) return 'No estás autenticado: inicia sesión de nuevo.';
    if (status === 403) return 'No tienes permisos para esta operación.';
    if (status === 404) return 'El recurso no existe.';
    return `Error ${status}`;
}

async function request(ruta, { method = 'GET', body, conToken = true } = {}) {
    const headers = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    const sesion = getSession();
    if (conToken && sesion) headers.Authorization = `Bearer ${sesion.token}`;

    let respuesta;
    try {
        respuesta = await fetch(`${API_BASE}${ruta}`, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
        });
    } catch {
        throw new ApiError(0, 'No se pudo conectar con el servidor. Comprueba que la API está arrancada en http://localhost:8080');
    }

    const texto = await respuesta.text();
    let datos = null;
    if (texto) {
        try {
            datos = JSON.parse(texto);
        } catch {
            datos = null;
        }
    }

    if (!respuesta.ok) {
        // Token caducado o inválido: no tiene sentido seguir en la página.
        if (respuesta.status === 401 && conToken) {
            clearSession();
            window.location.replace('index.html?sesion=expirada');
        }
        throw new ApiError(respuesta.status, mensajeDeError(respuesta.status, datos), datos);
    }
    return datos;
}

export const api = {
    login: (username, password) =>
        request('/auth/login', { method: 'POST', body: { username, password }, conToken: false }),

    forgotPassword: (email) =>
        request('/auth/forgot-password', { method: 'POST', body: { email }, conToken: false }),

    changePassword: (currentPassword, newPassword) =>
        request('/auth/change-password', { method: 'POST', body: { currentPassword, newPassword } }),

    registrarCliente: (datos) =>
        request('/clientes/register', { method: 'POST', body: datos, conToken: false }),

    miPerfil: () => request('/clientes/me'),

    actualizarPerfil: (datos) =>
        request('/clientes/me', { method: 'PUT', body: datos }),

    listarEmpleados: () => request('/admin/empleados'),

    obtenerEmpleado: (id) => request(`/admin/empleados/${id}`),

    crearEmpleado: (datos) =>
        request('/admin/empleados', { method: 'POST', body: datos }),
};