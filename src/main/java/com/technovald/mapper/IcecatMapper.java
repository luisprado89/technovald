package com.technovald.mapper;

import com.technovald.document.EspecificacionGrupo;
import com.technovald.document.EspecificacionItem;
import com.technovald.document.Imagen;
import com.technovald.document.ProductoDocumento;
import com.technovald.dto.icecat.IcecatFeature;
import com.technovald.dto.icecat.IcecatFeaturesGroups;
import com.technovald.dto.icecat.IcecatGallery;
import com.technovald.dto.icecat.IcecatGeneralInfo;
import com.technovald.dto.icecat.IcecatResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte la respuesta de Icecat en la ficha de MongoDB.
 *
 * <p>
 * El EAN es siempre el solicitado. Las especificaciones se guardan agrupadas y
 * las imágenes con sus dos resoluciones; la descripción larga usa el resumen de
 * Icecat cuando {@code LongDesc} viene vacío.
 * </p>
 */
@Component
public class IcecatMapper {

    /**
     * Construye o actualiza el documento a partir de la respuesta de Icecat.
     *
     * @param response respuesta de Icecat ya validada.
     * @param ean      EAN solicitado.
     * @param existente documento previo, o null si es nuevo.
     * @return el documento listo para guardar, sin fecha de sincronización.
     */
    public ProductoDocumento toDocumento(IcecatResponse response, String ean, ProductoDocumento existente) {
        ProductoDocumento documento = existente != null ? existente : new ProductoDocumento();
        IcecatGeneralInfo info = response.getData().getGeneralInfo();

        documento.setEan(ean);
        documento.setTitulo(texto(info != null ? info.getTitle() : null));
        documento.setNombreCorto(nombreCorto(info));
        documento.setMarca(texto(info != null ? info.getBrand() : null));
        documento.setCategoria(categoria(info));
        documento.setDescripcionCorta(descripcionCorta(info));
        documento.setDescripcionLarga(descripcionLarga(info));
        documento.setImagenes(imagenes(response.getData().getGallery()));
        documento.setEspecificaciones(especificaciones(response.getData().getFeaturesGroups()));

        return documento;
    }

    /** Nombre corto: ProductIntName → ProductName → Title. */
    private String nombreCorto(IcecatGeneralInfo info) {
        if (info == null) {
            return null;
        }
        if (info.getProductNameInfo() != null) {
            String corto = texto(info.getProductNameInfo().getProductIntName());
            if (corto != null) {
                return corto;
            }
        }
        String nombre = texto(info.getProductName());
        return nombre != null ? nombre : texto(info.getTitle());
    }

    /** Categoría: Category.Name.Value. */
    private String categoria(IcecatGeneralInfo info) {
        if (info == null || info.getCategory() == null || info.getCategory().getName() == null) {
            return null;
        }
        return texto(info.getCategory().getName().getValue());
    }

    /** Descripción corta: ShortSummaryDescription. */
    private String descripcionCorta(IcecatGeneralInfo info) {
        if (info == null || info.getSummaryDescription() == null) {
            return null;
        }
        return texto(info.getSummaryDescription().getShortSummaryDescription());
    }

    /**
     * Descripción larga: LongDesc → LongSummaryDescription → ShortSummaryDescription.
     */
    private String descripcionLarga(IcecatGeneralInfo info) {
        if (info == null) {
            return null;
        }
        if (info.getDescription() != null) {
            String larga = texto(info.getDescription().getLongDesc());
            if (larga != null) {
                return larga;
            }
        }
        if (info.getSummaryDescription() != null) {
            String larga = texto(info.getSummaryDescription().getLongSummaryDescription());
            if (larga != null) {
                return larga;
            }
            return texto(info.getSummaryDescription().getShortSummaryDescription());
        }
        return null;
    }

    /** Galería con alta, media y principal; si ninguna es principal, la primera. */
    private List<Imagen> imagenes(List<IcecatGallery> galeria) {
        if (galeria == null) {
            return List.of();
        }

        List<Imagen> resultado = new ArrayList<>();
        for (IcecatGallery imagen : galeria) {
            String alta = texto(imagen.getPic()) != null ? imagen.getPic().trim() : null;
            String media = texto(imagen.getPic500x500()) != null ? imagen.getPic500x500().trim() : null;

            if (alta == null && media == null) {
                continue;
            }
            resultado.add(Imagen.builder()
                    .urlAlta(alta != null ? alta : media)
                    .urlMedia(media)
                    .esPrincipal("Y".equalsIgnoreCase(imagen.getIsMain()))
                    .build());
        }

        if (!resultado.isEmpty() && resultado.stream().noneMatch(Imagen::isEsPrincipal)) {
            resultado.get(0).setEsPrincipal(true);
        }
        return resultado;
    }

    /** Grupos de especificaciones con sus items. */
    private List<EspecificacionGrupo> especificaciones(List<IcecatFeaturesGroups> grupos) {
        List<EspecificacionGrupo> resultado = new ArrayList<>();
        if (grupos == null) {
            return resultado;
        }

        for (IcecatFeaturesGroups grupo : grupos) {
            List<EspecificacionItem> items = new ArrayList<>();

            if (grupo.getFeatures() != null) {
                for (IcecatFeature feature : grupo.getFeatures()) {
                    String nombre = feature.getFeature() != null && feature.getFeature().getName() != null
                            ? texto(feature.getFeature().getName().getValue())
                            : null;
                    String valor = texto(feature.getPresentationValue());

                    if (nombre != null && valor != null) {
                        items.add(EspecificacionItem.builder().nombre(nombre).valor(valor).build());
                    }
                }
            }

            String nombreGrupo = grupo.getFeatureGroup() != null && grupo.getFeatureGroup().getName() != null
                    ? texto(grupo.getFeatureGroup().getName().getValue())
                    : null;

            if (!items.isEmpty()) {
                resultado.add(EspecificacionGrupo.builder()
                        .grupo(nombreGrupo != null ? nombreGrupo : "Especificaciones")
                        .items(items)
                        .build());
            }
        }

        return resultado;
    }

    /** Recorta y convierte vacíos en null. */
    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}