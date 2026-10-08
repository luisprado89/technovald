# TechNovaLD — API REST de E-commerce

Backend de la tienda TechNovaLD: una API REST en Spring Boot para gestionar
catálogo de productos, inventario, pedidos, reposiciones de stock y personal.

## 🛠️ Stack

| Elemento | Versión |
| --- | --- |
| Java | 17 |
| Spring Boot | 4.1.1 |
| Maven | 3.9.16 (vía Maven Wrapper) |
| MySQL | 8 o superior |

El `pom.xml` declara ya el conjunto completo de dependencias del proyecto.

## 📂 Estructura de carpetas

```
TechNovaLD/
├── .mvn/wrapper/
│   └── maven-wrapper.properties              # versión de Maven fijada (3.9.16)
├── src/
│   └── main/
│       ├── java/com/technovald/
│       │   ├── TechNovaLdApplication.java    # punto de entrada
│       │   ├── config/
│       │   │   └── SecurityConfig.java       # cadena de seguridad JWT
│       │   ├── controller/
│       │   │   └── AuthController.java       # POST /api/auth/login
│       │   ├── dto/
│       │   │   ├── request/LoginRequest.java
│       │   │   └── response/AuthResponse.java
│       │   ├── entity/                       # 10 entidades JPA (MySQL)
│       │   ├── enums/                        # RolUsuario, EstadoPedido, EstadoSolicitud
│       │   ├── repository/mysql/             # repositorios JPA
│       │   ├── security/
│       │   │   ├── CustomUserDetailsService.java
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   └── JwtService.java
│       │   ├── seeder/
│       │   │   └── AdminBootstrapInitializer.java
│       │   └── service/
│       │       ├── AuthService.java
│       │       └── impl/AuthServiceImpl.java
│       └── resources/
│           ├── application.properties        # configuración de la aplicación
│           └── db/migration/
│               └── V1__init_schema.sql       # las 10 tablas del modelo
├── .env.example                              # plantilla de variables de entorno
├── .gitattributes
├── .gitignore
├── mvnw / mvnw.cmd                           # lanzadores del wrapper
├── pom.xml
└── README.md
```

El fichero `.env` no aparece en la estructura porque está en `.gitignore`: es
local y nunca se sube al repositorio.

## 🚀 Puesta en marcha

### Requisitos previos

- **JDK 17** (`java -version`).
- **MySQL 8 o superior** en marcha; por defecto se espera en `localhost:3306`.
- No hace falta instalar Maven: `mvnw` / `mvnw.cmd` descargan la versión fijada
  en `.mvn/wrapper/maven-wrapper.properties` la primera vez que se ejecutan.

### 1. Crear el fichero de entorno

```powershell
Copy-Item .env.example .env
```

Y rellenar en `.env`:

- `MYSQL_PASSWORD` (y `MYSQL_USER` si no es `root`).
- `BOOTSTRAP_ADMIN_PASSWORD`: la contraseña del administrador inicial.
- `BOOTSTRAP_ADMIN_USERNAME`, si no quieres `admin.technovald`.
- `JWT_SECRET`: 32 bytes aleatorios en Base64. Para generar uno:

```powershell
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

- `JWT_EXPIRATION`: la vida del token en milisegundos (`86400000` son 24 horas).

Los valores de MySQL y de JWT no tienen valor por defecto a propósito: sin ellos
la aplicación no arranca.

### 2. Crear la base de datos

No hace falta crearla a mano: la URL de conexión incluye
`createDatabaseIfNotExist=true`, así que MySQL crea la base de datos en el
primer arranque. Si prefieres crearla tú:

```sql
CREATE DATABASE technovald
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

Aquí solo se crea la base de datos vacía: las tablas las versiona Flyway en el
arranque.

### 3. Arrancar la aplicación

```powershell
.\mvnw.cmd spring-boot:run
```

En el primer arranque sobre una base de datos vacía, Flyway aplica la migración
y se crea el administrador inicial. Un arranque correcto deja, entre otras,
estas líneas:

```
HikariPool-1 - Start completed.
Migrating schema `technovald` to version "1 - init schema"
Successfully applied 1 migration to schema `technovald`, now at version v1
=== BOOTSTRAP DE ADMINISTRADOR INICIAL ===
Administrador inicial creado con username 'admin.technovald'.
Tomcat started on port 8080 (http)
```

### 4. Compilar, empaquetar y ejecutar las pruebas

```powershell
.\mvnw.cmd clean package
```

Debe terminar en `BUILD SUCCESS` y generar `target/app.jar`. La prueba de
contexto (`TechNovaLdApplicationTests`) arranca la aplicación completa, así que
**necesita MySQL en marcha**.

### Problemas frecuentes

| Síntoma | Causa |
| --- | --- |
| `Could not resolve placeholder 'MYSQL_USER'` | falta el fichero `.env` |
| `Could not resolve placeholder 'JWT_SECRET'` | falta `JWT_SECRET` en `.env` |
| `WeakKeyException: The signing key's size is ... bits` | `JWT_SECRET` tiene menos de 32 bytes; genera otro |
| `Communications link failure` | MySQL no está en marcha, o no escucha en el puerto indicado |
| `Access denied for user` | `MYSQL_USER` o `MYSQL_PASSWORD` incorrectos en `.env` |
| `Bootstrap de administrador: la tabla usuario está vacía y faltan las credenciales` | la base de datos no tiene usuarios y faltan `BOOTSTRAP_ADMIN_*` en `.env` |
| `Found non-empty schema ... but no schema history table` | la base de datos ya tenía tablas de antes de que existiera Flyway; hay que vaciarla (`DROP DATABASE technovald;`) y volver a arrancar |
| `Migration checksum mismatch` | se editó una migración ya aplicada; hay que restaurarla tal cual estaba o recrear la base de datos |
| `Schema-validation: missing table [...]` o `wrong column type` | una entidad y su tabla no coinciden; hay que corregir la que esté mal |
| `401` en el login con credenciales que deberían ser correctas | contraseña equivocada, o el usuario está dado de baja (`activo = 0`) |

## 🗄️ Base de datos

### Migraciones

El esquema lo versiona **Flyway** con los scripts de
`src/main/resources/db/migration/`. Al arrancar, Flyway consulta la tabla
`flyway_schema_history` y aplica solo las migraciones que aún no se hayan
ejecutado, en orden de versión.

| Migración | Contenido |
| --- | --- |
| `V1__init_schema.sql` | las 10 tablas del modelo, con sus claves, índices y claves foráneas |

Reglas para añadir un cambio de esquema:

- Un cambio nuevo es **un fichero nuevo**: `V2__descripcion.sql`.
- Una migración ya aplicada **nunca se edita**: Flyway guarda su checksum y
  aborta el arranque si el fichero cambia.
- El nombre sigue el patrón `V<número>__<descripción>.sql`, con dos guiones
  bajos.
- Los tipos de las columnas se eligen para que coincidan con el mapeo por
  defecto de Hibernate sobre MySQL, que solo valida el esquema y no lo modifica.

### Entidades

El esquema está mapeado con JPA. Hibernate **no crea ni modifica tablas**
(`spring.jpa.hibernate.ddl-auto=validate`): al arrancar solo comprueba que cada
entidad coincide con la suya y aborta el arranque si encuentra cualquier
diferencia de tabla, columna o tipo.

| Entidad | Tabla | Qué representa |
| --- | --- | --- |
| `Persona` | `persona` | datos de identidad |
| `Usuario` | `usuario` | credenciales de acceso y rol |
| `Cliente` | `cliente` | perfil de compra |
| `Empleado` | `empleado` | perfil laboral |
| `Proveedor` | `proveedor` | empresa suministradora |
| `Categoria` | `categoria` | clasificación de productos |
| `ProductoInventario` | `producto_inventario` | stock y precios de un producto |
| `Pedido` | `pedido` | cabecera de una venta |
| `PedidoDetalle` | `pedido_detalle` | línea de un pedido |
| `SolicitudReposicion` | `solicitud_reposicion` | petición de reposición de stock |

Relaciones principales:

- `Persona` 1:1 con `Usuario`, `Cliente` y `Empleado`. La clave foránea vive en
  esas tres tablas, así que una persona puede ser cliente, empleado o ambas
  cosas sin duplicar sus datos.
- `Cliente` 1:N `Pedido`, y `Pedido` 1:N `PedidoDetalle`.
- `Categoria` 1:N `ProductoInventario` y `Proveedor` 1:N `ProductoInventario`.
- `SolicitudReposicion` apunta a un `ProductoInventario`, a un `Proveedor` y al
  `Empleado` que la creó.

Enums:

| Enum | Valores |
| --- | --- |
| `RolUsuario` | `ADMIN`, `TRABAJADOR`, `CLIENTE` |
| `EstadoPedido` | `PENDIENTE`, `PROCESANDO`, `ENVIADO`, `ENTREGADO` |
| `EstadoSolicitud` | `PENDIENTE`, `RECIBIDO` |

## 🔐 Autenticación

### Credenciales

Viven en la tabla `usuario`. La contraseña **nunca se guarda en claro**: se
almacena su hash BCrypt, que empieza por `$2a$`.

Al arrancar, la aplicación mira si la tabla `usuario` está vacía:

- **Vacía**: crea el administrador inicial con las credenciales de
  `BOOTSTRAP_ADMIN_USERNAME` y `BOOTSTRAP_ADMIN_PASSWORD`, le asigna el rol
  `ADMIN` y lo da de alta también en la tabla `empleado`.
- **Con usuarios**: no hace nada. El bootstrap es idempotente y nunca
  sobrescribe datos existentes.

Por eso esas dos variables solo hacen falta en el primer arranque: una vez
creado el administrador se pueden borrar del `.env`.

### Inicio de sesión

`POST /api/auth/login` es la única ruta pública: es la que reparte los tokens.
Recibe el usuario y la contraseña y, si son correctos, devuelve:

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "username": "admin.technovald",
  "rol": "ADMIN",
  "expiracion": "2026-10-09T21:30:00"
}
```

El token es un **JWT firmado en HMAC-SHA** con la clave de `JWT_SECRET`, lleva
el nombre de usuario como sujeto, el rol y la versión de credenciales vigente,
y caduca a los `JWT_EXPIRATION` milisegundos. No se guarda en ningún sitio: lo
que permite comprobar que lo emitió esta aplicación, y que nadie lo ha tocado,
es la firma.

### Peticiones autenticadas

El resto de rutas exigen el token en la cabecera `Authorization`:

```
Authorization: Bearer eyJhbGciOiJIUzM4NCJ9...
```

En cada petición, el filtro comprueba la firma, vuelve a cargar el usuario de la
base de datos y deja la petición autenticada. Si el token falta, está
manipulado, ha caducado, o su usuario ya no existe o está dado de baja, la
respuesta es **401**.

## 🧪 Probar la API con Postman

Las pruebas se hacen con **Postman** (o cualquier cliente HTTP). A continuación
están las peticiones con el **resultado real** de la Fase 6.

### Reglas generales

- En **todas** las peticiones: pestaña **`Authorization` → `No Auth`**.
- El token se manda **siempre en la cabecera `Authorization` manual**, no en la
  pestaña de Authorization de Postman.
- `<TOKEN>` = el `token` que devuelve la prueba 1.
- `<TOKEN_MANIPULADO>` = ese mismo token con **un carácter cambiado en la mitad**
  (no al final: los bits de padding del Base64 pueden hacer que la firma siga
  siendo válida si solo se añaden caracteres al final).

---

### 1. Login correcto → 200 + token

**Request**

| Campo | Valor |
| --- | --- |
| Método | `POST` |
| URL | `http://localhost:8080/api/auth/login` |
| Authorization | `No Auth` |
| Headers | `Content-Type: application/json` |
| Body | raw / JSON |

```json
{
  "username": "admin.technovald",
  "password": "admin"
}
```

**Response** — `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhZG1pbi50ZWNobm92YWxkIiwicm9sIjoiQURNSU4iLCJ2ZXIiOjAsImlhdCI6MTc5MTQ5MTM0MywiZXhwIjoxNzkxNTc3NzQzfQ.sheE-ENiif9XUN2BAjXp7ibSr7d2ztnoMUBEKJr_Zopkv1gxx6RdTMn9Bg5Us5L5",
  "username": "admin.technovald",
  "rol": "ADMIN",
  "expiracion": "2026-10-09T22:29:03"
}
```

---

### 2. Sin token → 401

**Request**

| Campo | Valor |
| --- | --- |
| Método | `GET` |
| URL | `http://localhost:8080/` |
| Authorization | `No Auth` |
| Headers | *ninguno* |
| Body | *ninguno* |

**Response** — `401 Unauthorized` (sin body)

---

### 3. Con token válido → 404

**Request**

| Campo | Valor |
| --- | --- |
| Método | `GET` |
| URL | `http://localhost:8080/` |
| Authorization | `No Auth` |
| Headers | `Authorization: Bearer <TOKEN>` |
| Body | *ninguno* |

**Response** — `404 Not Found`

```json
{
  "timestamp": "2026-10-08T20:31:58.275Z",
  "status": 404,
  "error": "Not Found",
  "path": "/"
}
```

> El **404** es el resultado correcto: la petición ha pasado la autenticación y
> ha llegado al dispatcher, que todavía no tiene ninguna ruta para `/`.

---

### 4. Con token manipulado → 401

**Request**

| Campo | Valor |
| --- | --- |
| Método | `GET` |
| URL | `http://localhost:8080/` |
| Authorization | `No Auth` |
| Headers | `Authorization: Bearer <TOKEN_MANIPULADO>` |
| Body | *ninguno* |

**Response** — `401 Unauthorized` (sin body)

---

### 5. Credenciales incorrectas → 401 ⚠️

**Request**

| Campo | Valor |
| --- | --- |
| Método | `POST` |
| URL | `http://localhost:8080/api/auth/login` |
| Authorization | `No Auth` |
| Headers | `Content-Type: application/json` |
| Body | raw / JSON |

```json
{
  "username": "admin.technovald",
  "password": "mal"
}
```

**Response actual** — `401 Unauthorized` (sin body)

> **Deuda conocida**: la respuesta correcta sería `400 Bad Request` con un
> cuerpo de error uniforme. Spring Security captura la `BadCredentialsException`
> antes de que llegue al dispatcher. Se resolverá cuando se añada un
> `GlobalExceptionHandler` propio en una fase posterior.

---

### 6. Campos vacíos → 400 ⚠️

**Request**

| Campo | Valor |
| --- | --- |
| Método | `POST` |
| URL | `http://localhost:8080/api/auth/login` |
| Authorization | `No Auth` |
| Headers | `Content-Type: application/json` |
| Body | raw / JSON |

```json
{
  "username": "",
  "password": ""
}
```

**Response actual** — `400 Bad Request`

```json
{
  "timestamp": "2026-10-08T20:34:07.506Z",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/auth/login"
}
```

> **Deuda conocida**: el cuerpo de error es el **por defecto de Spring Boot**,
> no un formato propio. Se unificará cuando se añada un
> `GlobalExceptionHandler` propio.

---

### Resumen de resultados

| # | Petición | Resultado | Nota |
| --- | --- | --- | --- |
| 1 | Login correcto | **200** + token | |
| 2 | Sin token | **401** | |
| 3 | Con token válido | **404** | |
| 4 | Con token manipulado | **401** | |
| 5 | Credenciales incorrectas | **401** | Deuda conocida: 400 con `GlobalExceptionHandler` |
| 6 | Campos vacíos | **400** | Deuda conocida: por defecto de Spring Boot |

## 📊 Estado del proyecto

| | |
| --- | --- |
| Compilación, pruebas y empaquetado | ✅ `.\mvnw.cmd clean package` |
| Arranque | ✅ arranca y conecta a MySQL |
| Esquema | ✅ 10 tablas versionadas por Flyway (1 migración) |
| Modelo JPA | ✅ 10 entidades y 3 enums, validados contra el esquema al arrancar |
| Autenticación | ✅ login con JWT; el resto de rutas exige `Authorization: Bearer` |
| Endpoints accesibles | `POST /api/auth/login` (público); ninguno más todavía |

## ⚠️ Deudas conocidas

- **Credenciales incorrectas en el login devuelven 401** en lugar de 400 con
  cuerpo uniforme. Spring Security captura la `BadCredentialsException` antes
  de que llegue al dispatcher. Se resolverá con un `GlobalExceptionHandler`
  propio.
- **El cuerpo de error de las respuestas 400 y 404 es el por defecto de Spring
  Boot**, no un formato propio. Se unificará con el mismo
  `GlobalExceptionHandler`.
- **Añadir un carácter Base64 válido al final de un token no invalida la
  firma** (los bits de padding del Base64 se ignoran). No es explotable: el
  token sigue siendo del mismo usuario, con la misma caducidad y la misma
  versión de credenciales. Se documenta como comportamiento conocido de JJWT.