package com.technovald.enums;

/**
 * Enumeración que define los roles de usuario disponibles en el sistema
 * TechNovaLD.
 *
 * <p>
 * Esta enumeración es la fuente de verdad para el control de acceso basado
 * en roles (RBAC) y se utiliza en dos lugares clave del modelo de datos:
 * </p>
 *
 * <ul>
 *     <li>
 *         {@code Usuario.rol}: rol almacenado en el token JWT y utilizado
 *         por Spring Security para autorizar o denegar el acceso a los
 *         endpoints. Es la fuente canónica para RBAC.
 *     </li>
 *     <li>
 *         {@code Empleado.rol}: sub-rol que distingue el tipo de empleado
 *         (administrador o trabajador). Permite consultar el tipo de
 *         empleado sin necesidad de realizar un JOIN con la tabla
 *         {@code usuario}. Es una desnormalización intencional.
 *     </li>
 * </ul>
 *
 * <p>
 * El rol {@code CLIENTE} nunca se asigna a un {@code Empleado}, ya que
 * los clientes se registran mediante un proceso público de auto-registro
 * y su perfil de dominio es {@code Cliente}, no {@code Empleado}.
 * </p>
 *
 * <p>
 * Modelo de permisos por rol:
 * </p>
 *
 * <ul>
 *     <li>{@code ADMIN}: acceso total. Gestiona empleados, categorías,
 *     proveedores, productos de inventario y sincroniza el catálogo con
 *     Icecat.</li>
 *     <li>{@code TRABAJADOR}: rol operativo. Gestiona compras de clientes,
 *     responde comentarios y crea solicitudes de reposición de stock.</li>
 *     <li>{@code CLIENTE}: usuario final. Se registra, compra productos,
 *     comenta y consulta su historial de pedidos.</li>
 * </ul>
 */
public enum RolUsuario {
    ADMIN,
    TRABAJADOR,
    CLIENTE
}
