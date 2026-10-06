-- ============================================================================
-- TechNovaLD · Esquema inicial (MySQL 8+)
-- ----------------------------------------------------------------------------
-- DDL generado con MySQL Workbench 8.0 a partir de la estructura creada por
-- Hibernate con ddl-auto=update, revisado y ordenado manualmente para que las
-- claves foráneas se creen después de sus tablas referenciadas.
--
-- Exportado desde: Server → Data Export → Dump Structure Only
-- Revisado: sin DROP TABLE, sin flyway_schema_history
-- ============================================================================

--
-- Table structure for table `persona`
--

CREATE TABLE `persona`
(
    `id`        bigint       NOT NULL AUTO_INCREMENT,
    `nombre`    varchar(255) NOT NULL,
    `apellido`  varchar(255) NOT NULL,
    `dni`       varchar(9)   NOT NULL,
    `email`     varchar(255) NOT NULL,
    `telefono`  varchar(255) NOT NULL,
    `direccion` varchar(255) NOT NULL,
    `activo`    bit(1)       NOT NULL DEFAULT b'1',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_persona_dni` (`dni`),
    UNIQUE KEY `uk_persona_email` (`email`),
    KEY         `idx_persona_activo` (`activo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


--
-- Table structure for table `usuario`
--

CREATE TABLE `usuario`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `username`       varchar(100) NOT NULL,
    `password`       varchar(255) NOT NULL,
    `rol`            varchar(50)  NOT NULL,
    `persona_id`     bigint       NOT NULL,
    `fecha_creacion` datetime(6) NOT NULL,
    `activo`         bit(1)       NOT NULL DEFAULT b'1',
    `token_version`  int          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_usuario_username` (`username`),
    UNIQUE KEY `uk_usuario_persona` (`persona_id`),
    KEY              `idx_usuario_activo` (`activo`),
    CONSTRAINT `fk_usuario_persona` FOREIGN KEY (`persona_id`) REFERENCES `persona` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


--
-- Table structure for table `cliente`
--

CREATE TABLE `cliente`
(
    `id`         bigint NOT NULL AUTO_INCREMENT,
    `persona_id` bigint NOT NULL,
    `activo`     bit(1) NOT NULL DEFAULT b'1',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cliente_persona` (`persona_id`),
    KEY          `idx_cliente_activo` (`activo`),
    CONSTRAINT `fk_cliente_persona` FOREIGN KEY (`persona_id`) REFERENCES `persona` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


--
-- Table structure for table `proveedor`
--

CREATE TABLE `proveedor`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `nombre_empresa` varchar(255) NOT NULL,
    `contacto`       varchar(255) NOT NULL,
    `telefono`       varchar(255) NOT NULL,
    `email`          varchar(255) NOT NULL,
    `direccion`      varchar(255) NOT NULL,
    `activo`         bit(1)       NOT NULL DEFAULT b'1',
    PRIMARY KEY (`id`),
    KEY              `idx_proveedor_activo` (`activo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



--
-- Table structure for table `categoria`
--

CREATE TABLE `categoria`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT,
    `nombre`      varchar(255) NOT NULL,
    `descripcion` varchar(255)          DEFAULT NULL,
    `activo`      bit(1)       NOT NULL DEFAULT b'1',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_categoria_nombre` (`nombre`),
    KEY           `idx_categoria_activo` (`activo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `empleado`
--


CREATE TABLE `empleado`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT,
    `persona_id` bigint      NOT NULL,
    `rol`        varchar(50) NOT NULL,
    `activo`     bit(1)      NOT NULL DEFAULT b'1',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_empleado_persona` (`persona_id`),
    KEY          `idx_empleado_activo` (`activo`),
    CONSTRAINT `fk_empleado_persona` FOREIGN KEY (`persona_id`) REFERENCES `persona` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


--
-- Table structure for table `producto_inventario`
--

CREATE TABLE `producto_inventario`
(
    `id`            bigint         NOT NULL AUTO_INCREMENT,
    `ean`           varchar(255)   NOT NULL,
    `nombre_corto`  varchar(255)   NOT NULL,
    `stock_actual`  int            NOT NULL,
    `stock_minimo`  int            NOT NULL,
    `precio_compra` decimal(10, 2) NOT NULL,
    `precio_venta`  decimal(10, 2) NOT NULL,
    `marca`         varchar(100)   NOT NULL,
    `categoria_id`  bigint         NOT NULL,
    `proveedor_id`  bigint         NOT NULL,
    `activo`        bit(1)         NOT NULL DEFAULT b'1',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_producto_ean` (`ean`),
    KEY             `idx_producto_activo` (`activo`),
    KEY             `idx_producto_categoria_activo` (`categoria_id`,`activo`),
    KEY             `idx_producto_proveedor_activo` (`proveedor_id`,`activo`),
    CONSTRAINT `fk_producto_categoria` FOREIGN KEY (`categoria_id`) REFERENCES `categoria` (`id`),
    CONSTRAINT `fk_producto_proveedor` FOREIGN KEY (`proveedor_id`) REFERENCES `proveedor` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


--
-- Table structure for table `pedido`
--

CREATE TABLE `pedido`
(
    `id`           bigint         NOT NULL AUTO_INCREMENT,
    `fecha_pedido` datetime(6) NOT NULL,
    `estado`       varchar(50)    NOT NULL DEFAULT 'PENDIENTE',
    `total`        decimal(10, 2) NOT NULL,
    `cliente_id`   bigint         NOT NULL,
    `activo`       bit(1)         NOT NULL DEFAULT b'1',
    PRIMARY KEY (`id`),
    KEY            `idx_pedido_activo` (`activo`),
    KEY            `idx_pedido_cliente_activo` (`cliente_id`,`activo`),
    KEY            `idx_pedido_estado_activo` (`estado`,`activo`),
    CONSTRAINT `fk_pedido_cliente` FOREIGN KEY (`cliente_id`) REFERENCES `cliente` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


--
-- Table structure for table `pedido_detalle`
--

CREATE TABLE `pedido_detalle`
(
    `id`              bigint         NOT NULL AUTO_INCREMENT,
    `pedido_id`       bigint         NOT NULL,
    `producto_id`     bigint         NOT NULL,
    `cantidad`        int            NOT NULL,
    `precio_unitario` decimal(10, 2) NOT NULL,
    `subtotal`        decimal(10, 2) NOT NULL,
    `activo`          bit(1)         NOT NULL DEFAULT b'1',
    PRIMARY KEY (`id`),
    KEY               `fk_detalle_pedido` (`pedido_id`),
    KEY               `fk_detalle_producto` (`producto_id`),
    CONSTRAINT `fk_detalle_pedido` FOREIGN KEY (`pedido_id`) REFERENCES `pedido` (`id`),
    CONSTRAINT `fk_detalle_producto` FOREIGN KEY (`producto_id`) REFERENCES `producto_inventario` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


--
-- Table structure for table `solicitud_reposicion`
--

CREATE TABLE `solicitud_reposicion`
(
    `id`              bigint      NOT NULL AUTO_INCREMENT,
    `fecha_solicitud` datetime(6) NOT NULL,
    `cantidad`        int         NOT NULL,
    `estado`          varchar(50) NOT NULL DEFAULT 'PENDIENTE',
    `producto_id`     bigint      NOT NULL,
    `proveedor_id`    bigint      NOT NULL,
    `empleado_id`     bigint      NOT NULL,
    `activo`          bit(1)      NOT NULL DEFAULT b'1',
    PRIMARY KEY (`id`),
    KEY               `fk_solicitud_proveedor` (`proveedor_id`),
    KEY               `fk_solicitud_empleado` (`empleado_id`),
    KEY               `idx_solicitud_activo` (`activo`),
    KEY               `idx_solicitud_estado_activo` (`estado`,`activo`),
    KEY               `idx_solicitud_producto_activo` (`producto_id`,`activo`),
    CONSTRAINT `fk_solicitud_empleado` FOREIGN KEY (`empleado_id`) REFERENCES `empleado` (`id`),
    CONSTRAINT `fk_solicitud_producto` FOREIGN KEY (`producto_id`) REFERENCES `producto_inventario` (`id`),
    CONSTRAINT `fk_solicitud_proveedor` FOREIGN KEY (`proveedor_id`) REFERENCES `proveedor` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
