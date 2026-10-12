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
│   ├── main/
│   │   ├── java/com/technovald/
│   │   │   ├── TechNovaLdApplication.java    # punto de entrada
│   │   │   ├── config/
│   │   │   │   └── SecurityConfig.java       # cadena de seguridad JWT
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java       # POST /api/auth/login
│   │   │   │   └── ClienteController.java    # POST /api/clientes/register
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   ├── ClienteRegistroRequest.java
│   │   │   │   │   └── LoginRequest.java
│   │   │   │   └── response/
│   │   │   │       ├── AuthResponse.java
│   │   │   │       └── ClienteRegistroResponse.java
│   │   │   ├── entity/                       # 10 entidades JPA (MySQL)
│   │   │   ├── enums/                        # RolUsuario, EstadoPedido, EstadoSolicitud
│   │   │   ├── exception/
│   │   │   │   ├── BusinessRuleException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── mapper/
│   │   │   │   └── PersonaMapper.java
│   │   │   ├── repository/mysql/             # repositorios JPA
│   │   │   ├── security/
│   │   │   │   ├── CredentialGenerator.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── JwtService.java
│   │   │   ├── seeder/
│   │   │   │   └── AdminBootstrapInitializer.java
│   │   │   └── service/
│   │   │       ├── AuthService.java
│   │   │       ├── ClienteService.java
│   │   │       └── impl/
│   │   │           ├── AuthServiceImpl.java
│   │   │           └── ClienteServiceImpl.java
│   │   └── resources/
│   │       ├── application.properties        # configuración de la aplicación
│   │       └── db/migration/
│   │           └── V1__init_schema.sql       # las 10 tablas del modelo
│   └── test/
│       └── java/com/technovald/
│           └── TechNovaLdApplicationTests.java   # test de contexto
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

`POST /api/auth/login` es una de las rutas públicas: reparte los tokens. Recibe
el usuario y la contraseña y, si son correctos, devuelve:

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

## 🧾 Registro de clientes

`POST /api/clientes/register` es la segunda ruta pública de la API: da de alta
a un cliente que todavía no tiene credenciales. No recibe usuario ni
contraseña; recibe los datos personales y el sistema genera las credenciales.

### Qué se crea

En una sola transacción se persisten tres filas:

| Tabla | Qué se guarda |
| --- | --- |
| `persona` | nombre, apellido, DNI, email, teléfono y dirección |
| `usuario` | username generado, hash BCrypt de la contraseña generada y rol `CLIENTE` |
| `cliente` | el perfil de compra, enlazado con su persona |

Si algo falla a mitad, no queda ninguna de las tres: o se crea el cliente
completo, o no se crea nada.

### Cómo se generan las credenciales

- **Username**: `nombre.apellido`, en minúsculas, sin acentos y solo con
  `[a-z0-9]` (José García → `jose.garcia`). Si ese username ya está ocupado se
  prueba `jose.garcia1`, `jose.garcia2`… hasta 100.000 candidatos.
- **Contraseña**: 16 caracteres aleatorios (criptográficamente seguros) con al
  menos una mayúscula, una minúscula, un dígito y un símbolo **garantizados por
  construcción**, no por probabilidad. Se excluyen los caracteres ambiguos
  (`I`, `O`, `l`, `0`, `1`) y los que rompen JSON, HTML, shell o `.env`
  (comillas, barras, `< > &`, `$`).
- **Hash**: en la tabla `usuario` solo se guarda el hash BCrypt. La contraseña
  en claro se devuelve **una sola vez**, en la respuesta del alta, y no se
  escribe en ningún log.

La política (longitud 16, 100.000 intentos de username) vive en las constantes de
`CredentialGenerator`: son valores fijos del sistema, no configurables, porque
ningún entorno necesita hoy otros distintos.

### Respuesta — `201 Created`

```json
{
  "id": 2,
  "nombre": "Lucia",
  "apellido": "Garcia Perez",
  "username": "lucia.garcia",
  "passwordInicial": "7N8u*yT-nY**%mCv",
  "rol": "CLIENTE",
  "token": "eyJhbGciOiJIUzM4NCJ9..."
}
```

El `token` es un JWT con la misma forma que el del login, así que el cliente
queda autenticado nada más registrarse, sin pasar por
`POST /api/auth/login` (aunque puede hacerlo: las credenciales funcionan).

La respuesta **no** lleva cabecera `Location`: todavía no hay un endpoint que
consulte un cliente por su id, y apuntar a una ruta que responde 404 sería peor
que no apuntar a ninguna.

### Reglas de negocio

| Caso | Respuesta |
| --- | --- |
| DNI ya registrado | **400** `Ya existe una persona registrada con el DNI indicado` |
| Email ya registrado | **400** `Ya existe una persona registrada con el email indicado` |
| DNI con formato inválido, campos vacíos o email ilegible | **400** con `campo: motivo` por cada campo |
| Cuerpo ausente o JSON roto | **400** `El cuerpo de la petición no es un JSON válido o está vacío` |

Todos los errores llegan con el formato uniforme de la sección siguiente.

## ⚠️ Formato de error uniforme

Todas las respuestas de error de la API siguen la misma estructura, definida
por el `record` anidado `GlobalExceptionHandler.ErrorResponse`:

```json
{
  "timestamp": "2026-10-11T12:28:02.623386",
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciales inválidas",
  "path": "/api/auth/login"
}
```

### Traducción de excepciones a códigos HTTP

| Excepción | Status | Cuándo se lanza |
| --- | --- | --- |
| `ResourceNotFoundException` | **404** | Recurso no encontrado o inactivo |
| `BusinessRuleException` | **400** | Regla de negocio infringida |
| `MethodArgumentNotValidException` | **400** | Validación de DTO con `@Valid` fallida |
| `AuthenticationException` | **401** | Credenciales incorrectas en el login |
| `HttpMessageNotReadableException` | **400** | JSON ilegible o cuerpo ausente |
| `NoResourceFoundException` | **404** | Ruta inexistente |
| Excepciones de Spring MVC | **su status** | 405, 415, 400... respetados |
| Cualquier otra | **500** | Error interno con mensaje genérico |

### Nota sobre los 401 de la cadena de filtros

Los **401 que devuelve el `JwtAuthenticationFilter`** (token ausente,
manipulado o caducado) **no pasan por el `GlobalExceptionHandler`**: Spring
Security resuelve la petición antes de que llegue al `DispatcherServlet`. Esos
401 se devuelven **sin cuerpo**. Los 401 del login (credenciales incorrectas),
en cambio, sí pasan por el handler y llevan el formato uniforme.

## 🧪 Probar la API con Postman

Las pruebas se hacen con **Postman** (o cualquier cliente HTTP).

### Reglas generales

- En **todas** las peticiones: pestaña **`Authorization` → `No Auth`**.
- El token se manda **siempre en la cabecera `Authorization` manual**, no en la
  pestaña de Authorization de Postman.
- `<TOKEN>` = el `token` que devuelve la prueba 1.

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
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "username": "admin.technovald",
  "rol": "ADMIN",
  "expiracion": "2026-10-09T22:29:03"
}
```

### 2. Sin token → 401 (vacío)

**Request**: `GET http://localhost:8080/`, sin Authorization.

**Response** — `401 Unauthorized`, sin body.

### 3. Con token válido → 404 (formato uniforme)

**Request**: `GET http://localhost:8080/` con `Authorization: Bearer <TOKEN>`.

**Response** — `404 Not Found`

```json
{
  "timestamp": "2026-10-11T12:30:52.856150",
  "status": 404,
  "error": "Not Found",
  "message": "El endpoint solicitado no existe: /",
  "path": "/"
}
```

### 4. Registro correcto → 201

**Request**

| Campo | Valor |
| --- | --- |
| Método | `POST` |
| URL | `http://localhost:8080/api/clientes/register` |
| Authorization | `No Auth` |
| Headers | `Content-Type: application/json` |

```json
{
  "nombre": "Lucia",
  "apellido": "Garcia Perez",
  "dni": "12345678Z",
  "email": "lucia.garcia@technovald.test",
  "telefono": "600111222",
  "direccion": "Calle Prueba 1"
}
```

**Response** — `201 Created`

```json
{
  "id": 1,
  "nombre": "Lucia",
  "apellido": "Garcia Perez",
  "username": "lucia.garcia",
  "passwordInicial": "7N8u*yT-nY**%mCv",
  "rol": "CLIENTE",
  "token": "eyJhbGciOiJIUzM4NCJ9..."
}
```

### 5. DNI repetido → 400

**Request**: mismo DNI, otro email.

**Response** — `400 Bad Request`

```json
{
  "message": "Ya existe una persona registrada con el DNI indicado"
}
```

### 6. Email repetido → 400

**Request**: otro DNI, mismo email.

**Response** — `400 Bad Request`

```json
{
  "message": "Ya existe una persona registrada con el email indicado"
}
```

### 7. Username con sufijo → 201

**Request**: mismos nombre y apellido, otros DNI y email.

**Response** — `201 Created` con `"username": "lucia.garcia1"`.

### 8. Login con las credenciales generadas → 200

**Request**: `POST /api/auth/login` con el `username` y la `passwordInicial` de la prueba 4.

**Response** — `200 OK` con `"rol": "CLIENTE"`.

### 9. Datos inválidos → 400

**Request**

```json
{
  "nombre": "",
  "apellido": "Perez",
  "dni": "123",
  "email": "no-es-un-email",
  "telefono": "",
  "direccion": ""
}
```

**Response** — `400 Bad Request` con `message` que une los errores de todos los
campos: `"dni: El DNI debe tener 8 números seguidos de una letra mayúscula,
email: El email debe tener un formato válido, nombre: El nombre es obligatorio,
direccion: La dirección es obligatoria, telefono: El teléfono es obligatorio"`.

### Resumen de resultados

| # | Petición | Resultado | Formato uniforme |
| --- | --- | --- | --- |
| 1 | Login correcto | **200** + token | — |
| 2 | Sin token | **401** | ❌ (viene de los filtros) |
| 3 | Con token válido | **404** | ✅ |
| 4 | Registro correcto | **201** + credenciales | — |
| 5 | DNI repetido | **400** | ✅ |
| 6 | Email repetido | **400** | ✅ |
| 7 | Username con sufijo | **201** | — |
| 8 | Login con las credenciales generadas | **200** + token | — |
| 9 | Datos inválidos | **400** | ✅ |

## 📊 Estado del proyecto

| | |
| --- | --- |
| Compilación, pruebas y empaquetado | ✅ `.\mvnw.cmd clean package` |
| Arranque | ✅ arranca y conecta a MySQL |
| Esquema | ✅ 10 tablas versionadas por Flyway (1 migración) |
| Modelo JPA | ✅ 10 entidades y 3 enums, validados contra el esquema al arrancar |
| Autenticación | ✅ login con JWT; el resto de rutas exige `Authorization: Bearer` |
| Manejo de errores | ✅ formato uniforme en todos los errores que pasan por el dispatcher |
| Registro de clientes | ✅ alta pública transaccional con credenciales generadas y token |
| Endpoints accesibles | `POST /api/auth/login` y `POST /api/clientes/register` (públicos); ninguno más todavía |

## ⚠️ Deudas conocidas

- **Dos altas simultáneas con el mismo DNI o email** pueden pasar las dos la
  comprobación previa y dejar que sea MySQL quien rechace el segundo `INSERT`
  por la restricción `UNIQUE`: esa petición recibe un `500` en lugar del `400`
  de negocio. La ventana es mínima y el dato **nunca** queda duplicado, porque
  la garantía real es la restricción de la base de datos.
- **La contraseña inicial viaja en claro** en la respuesta del alta: es la
  única forma de que el cliente la conozca. En la base de datos solo queda su
  hash BCrypt.
- **La política de credenciales es fija** (longitud 16, 100.00000 intentos de
  username) en las constantes de `CredentialGenerator`. Cuando existan perfiles
  `dev`/`prod`, se externalizará a `application.properties`.
- **La validación del DNI solo comprueba el formato** (8 dígitos + letra
  mayúscula), no que la letra corresponda con los dígitos según el algoritmo
  oficial.
- **Añadir un carácter Base64 válido al final de un token no invalida la
  firma** (los bits de padding del Base64 se ignoran). No es explotable: el
  token sigue siendo del mismo usuario, con la misma caducidad y la misma
  versión de credenciales. Se documenta como comportamiento conocido de JJWT.
- **Los 401 de la cadena de filtros no llevan cuerpo** (ni formato uniforme):
  Spring Security resuelve la petición antes del dispatcher, así que el
  `GlobalExceptionHandler` no puede darles cuerpo.