package com.technovald.mapper;

import com.technovald.document.ProductoDocumento;
import com.technovald.dto.icecat.IcecatFeature;
import com.technovald.dto.icecat.IcecatFeaturesGroups;
import com.technovald.dto.icecat.IcecatGallery;
import com.technovald.dto.icecat.IcecatGeneralInfo;
import com.technovald.dto.icecat.IcecatResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convierte la respuesta de Icecat en la ficha plana de MongoDB.
 *
 * <p>
 * Solo traslada lo que el documento guarda: nombre, marca, categoría,
 * descripción, URLs de imágenes y especificaciones aplanadas. El EAN es
 * siempre el solicitado, no el que devuelva Icecat, porque es el puente con
 * el inventario.
 * </p>
 */
@Component
public class IcecatMapper {

    /**
     * Construye o actualiza el documento a partir de la respuesta de Icecat.
     *
     * @param response respuesta de Icecat ya validada por el cliente.
     * @param ean      EAN solicitado; se impone sobre el de Icecat.
     * @param existente documento previo si la ficha ya existía; puede ser null.
     * @return el documento listo para guardar, sin fechas: las pone el servicio.
     */
    public ProductoDocumento toDocumento(IcecatResponse response, String ean, ProductoDocumento existente) {
        ProductoDocumento documento = existente != null ? existente : new ProductoDocumento();
        IcecatGeneralInfo info = response.getData().getGeneralInfo();

        documento.setEan(ean);
        documento.setNombre(texto(info != null ? info.getTitle() : null));
        documento.setMarca(texto(info != null ? info.getBrand() : null));
        documento.setCategoria(categoria(info));
        documento.setDescripcionLarga(descripcion(info));
        documento.setImagenes(imagenes(response.getData().getGallery()));
        documento.setEspecificaciones(especificaciones(response.getData().getFeaturesGroups()));

        return documento;
    }

    /**
     * Extrae {@code Category.Name.Value}.
     *
     * @param info datos generales; puede ser null.
     * @return el nombre de la categoría, o null si no viene.
     */
    private String categoria(IcecatGeneralInfo info) {
        if (info == null || info.getCategory() == null || info.getCategory().getName() == null) {
            return null;
        }
        return texto(info.getCategory().getName().getValue());
    }

    /**
     * Extrae {@code Description.LongDesc}.
     *
     * @param info datos generales; puede ser null.
     * @return la descripción HTML, o null si no viene.
     */
    private String descripcion(IcecatGeneralInfo info) {
        if (info == null || info.getDescription() == null) {
            return null;
        }
        return texto(info.getDescription().getLongDesc());
    }

    /**
     * Convierte la galería en una lista de URLs, con la imagen principal
     * primero. Si una imagen no trae {@code Pic}, se usa {@code Pic500x500}.
     *
     * @param galeria imágenes de Icecat; puede ser null.
     * @return las URLs, sin huecos ni nulos.
     */
    private List<String> imagenes(List<IcecatGallery> galeria) {
        if (galeria == null) {
            return List.of();
        }

        List<String> principales = new ArrayList<>();
        List<String> resto = new ArrayList<>();

        for (IcecatGallery imagen : galeria) {
            String url = texto(imagen.getPic()) != null ? imagen.getPic() : imagen.getPic500x500();
            if (texto(url) == null) {
                continue;
            }
            if ("Y".equalsIgnoreCase(imagen.getIsMain())) {
                principales.add(url.trim());
            } else {
                resto.add(url.trim());
            }
        }

        principales.addAll(resto);
        return principales;
    }

    /**
     * Aplana los grupos en un mapa nombre → valor. Si dos grupos repiten la
     * misma clave, se queda la última.
     *
     * @param grupos grupos de especificaciones; puede ser null.
     * @return el mapa de especificaciones, en el orden en que llegan.
     */
    private Map<String, String> especificaciones(List<IcecatFeaturesGroups> grupos) {
        Map<String, String> especificaciones = new LinkedHashMap<>();
        if (grupos == null) {
            return especificaciones;
        }

        for (IcecatFeaturesGroups grupo : grupos) {
            if (grupo.getFeatures() == null) {
                continue;
            }
            for (IcecatFeature feature : grupo.getFeatures()) {
                String nombre = feature.getFeature() != null && feature.getFeature().getName() != null
                        ? texto(feature.getFeature().getName().getValue())
                        : null;
                String valor = texto(feature.getPresentationValue());
                if (nombre != null && valor != null) {
                    especificaciones.put(nombre, valor);
                }
            }
        }

        return especificaciones;
    }

    /**
     * Recorta un texto y convierte los vacíos en null.
     *
     * @param valor texto original; puede ser null.
     * @return el texto limpio, o null si quedaba vacío.
     */
    private String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}