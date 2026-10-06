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
│       │   ├── entity/                       # 10 entidades JPA (MySQL)
│       │   └── enums/                        # RolUsuario, EstadoPedido, EstadoSolicitud
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

Después, rellenar en `.env` el valor de `MYSQL_PASSWORD` (y `MYSQL_USER` si no
es `root`). Sin ese fichero la aplicación no arranca: las credenciales no
tienen valor por defecto a propósito.

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

En el primer arranque sobre una base de datos vacía, Flyway aplica la
migración pendiente. Un arranque correcto deja, entre otras, estas líneas:

```
HikariPool-1 - Start completed.
Migrating schema `technovald` to version "1 - init schema"
Successfully applied 1 migration to schema `technovald`, now at version v1
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
| `Communications link failure` | MySQL no está en marcha, o no escucha en el puerto indicado |
| `Access denied for user` | `MYSQL_USER` o `MYSQL_PASSWORD` incorrectos en `.env` |
| `Found non-empty schema ... but no schema history table` | la base de datos ya tenía tablas de antes de que existiera Flyway; hay que vaciarla (`DROP DATABASE technovald;`) y volver a arrancar |
| `Migration checksum mismatch` | se editó una migración ya aplicada; hay que restaurarla tal cual estaba o recrear la base de datos |
| `Schema-validation: missing table [...]` o `wrong column type` | una entidad y su tabla no coinciden; hay que corregir la que esté mal |

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

## 📊 Estado del proyecto

| | |
| --- | --- |
| Compilación, pruebas y empaquetado | ✅ `.\mvnw.cmd clean package` |
| Arranque | ✅ arranca y conecta a MySQL |
| Esquema | ✅ 10 tablas versionadas por Flyway (1 migración) |
| Modelo JPA | ✅ 10 entidades y 3 enums, validados contra el esquema al arrancar |
| Endpoints accesibles | ninguno todavía |