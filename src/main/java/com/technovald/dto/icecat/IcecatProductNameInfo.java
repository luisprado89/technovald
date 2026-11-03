package com.technovald.dto.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/** Nombre corto internacional del producto. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatProductNameInfo {

    /** Nombre corto, por ejemplo "65QNED87B6A". */
    private String ProductIntName;
}