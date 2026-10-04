# TechNovaLD — API REST de E-commerce

Backend de la tienda TechNovaLD: una API REST en Spring Boot para gestionar
catálogo de productos, inventario, pedidos, reposiciones de stock y personal.

## 🛠️ Stack

| Elemento | Versión |
| --- | --- |
| Java | 17 |
| Spring Boot | 4.1.1 |
| Maven | 3.9.16 (vía Maven Wrapper) |

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
├── .env.example
├── .gitattributes
├── .gitignore
├── mvnw / mvnw.cmd                           # lanzadores del wrapper
├── pom.xml
└── README.md
```

## 🚀 Puesta en marcha

### Requisitos previos

- **JDK 17** (`java -version`).
- No hace falta instalar Maven: `mvnw` / `mvnw.cmd` descargan la versión fijada
  en `.mvn/wrapper/maven-wrapper.properties` la primera vez que se ejecutan.

### Compilar y empaquetar

```powershell
.\mvnw.cmd clean package
```

Debe terminar en `BUILD SUCCESS` y generar `target/app.jar`.

### Arrancar

```powershell
.\mvnw.cmd spring-boot:run
```

⚠️ **Todavía no arranca, y es lo esperado.** El arranque termina con:

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Failed to configure a DataSource: 'url' attribute is not specified and no
embedded datasource could be configured.
```

El `pom.xml` ya incluye `spring-boot-starter-data-jpa`, así que Spring Boot
intenta autoconfigurar un `DataSource` al arrancar, y `application.properties`
todavía no declara ninguna base de datos. Hasta que exista esa configuración,
el proyecto **compila y se empaqueta**, pero no arranca.

## 📊 Estado del proyecto

| | |
| --- | --- |
| Compilación y empaquetado | ✅ `.\mvnw.cmd clean package` |
| Arranque | ❌ pendiente de configurar la base de datos |
| Endpoints disponibles | ninguno todavía |