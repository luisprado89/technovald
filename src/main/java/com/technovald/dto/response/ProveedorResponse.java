package com.technovald.dto.response;

import lombok.*;

/** Datos de un proveedor devueltos al panel de administración. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProveedorResponse {

    /** Identificador único del proveedor. */
    private Long id;

    /** Nombre de la empresa proveedora. */
    private String nombreEmpresa;

    /** CIF de la empresa proveedora. */
    private String cif;

    /** Persona de contacto en la empresa. */
    private String contacto;

    /** Teléfono de contacto. */
    private String telefono;

    /** Correo electrónico de contacto. */
    private String email;

    /** Dirección física o fiscal. */
    private String direccion;

    /** Estado lógico: true si está activo, false si ha sido dado de baja. */
    private Boolean activo;
}