-- CIF del proveedor, único. Nullable para no romper filas ya existentes.
ALTER TABLE proveedor
    ADD COLUMN cif VARCHAR(20) NULL AFTER nombre_empresa,
    ADD UNIQUE KEY uk_proveedor_cif (cif);