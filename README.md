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
│       │   └── TechNovaLdApplication.java    # punto de entrada
│       └── resources/
│           └── application.properties        # configuración de la aplicación
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

### 2. Base de datos

No hace falta crearla a mano: la URL de conexión incluye
`createDatabaseIfNotExist=true`, así que MySQL crea `technovald` en el primer
arranque. Si prefieres crearla tú:

```sql
CREATE DATABASE technovald
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

### 3. Arrancar la aplicación

```powershell
.\mvnw.cmd spring-boot:run
```

Un arranque correcto deja, entre otras, estas dos líneas en el log:

```
HikariPool-1 - Start completed.
Tomcat started on port 8080 (http)
```

### 4. Compilar, empaquetar y ejecutar las pruebas

```powershell
.\mvnw.cmd clean package
```

Debe terminar en `BUILD SUCCESS` y generar `target/app.jar`. La prueba de
contexto (`TechNovaLdApplicationTests`) arranca la aplicación completa, así que
**necesita MySQL en marcha**: es justamente la prueba que confirma que la
conexión está bien configurada.

### Problemas frecuentes

| Síntoma | Causa |
| --- | --- |
| `Could not resolve placeholder 'MYSQL_USER'` | falta el fichero `.env` |
| `Communications link failure` | MySQL no está en marcha, o no escucha en el puerto indicado |
| `Access denied for user` | `MYSQL_USER` o `MYSQL_PASSWORD` incorrectos en `.env` |

## 📊 Estado del proyecto

| | |
| --- | --- |
| Compilación, pruebas y empaquetado | ✅ `.\mvnw.cmd clean package` |
| Arranque | ✅ arranca y conecta a MySQL |
| Endpoints accesibles | ninguno todavía |