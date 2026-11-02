package com.technovald.client;

import com.technovald.dto.icecat.IcecatResponse;
import com.technovald.exception.IcecatApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP de Icecat. Consulta la ficha de un producto por su EAN usando
 * la URL base de {@code icecat.api.url} y el token de {@code icecat.api.token}
 * en la cabecera {@code Api-Token}.
 *
 * <p>
 * Cualquier fallo del servicio externo se traduce a {@link IcecatApiException}
 * para que el {@code GlobalExceptionHandler} responda 503 y no un 500 genérico.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IcecatClient {

    /** RestTemplate con timeouts, configurado en {@code RestTemplateConfig}. */
    private final RestTemplate restTemplate;

    /** URL base de Icecat; termina en {@code GTIN=}. */
    @Value("${icecat.api.url}")
    private String baseUrl;

    /** Token de la cuenta; se envía en la cabecera {@code Api-Token}. */
    @Value("${icecat.api.token}")
    private String token;

    /**
     * Consulta la ficha de un producto en Icecat.
     *
     * @param ean código de barras del producto.
     * @return la respuesta de Icecat, con {@code data} distinto de null.
     * @throws IcecatApiException si Icecat no responde, devuelve un error HTTP
     *                            o no tiene ficha para ese EAN.
     */
    public IcecatResponse obtenerProducto(String ean) {
        String url = baseUrl + ean;
        log.info("Consultando Icecat para el EAN {}", ean);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Api-Token", token);

        try {
            ResponseEntity<IcecatResponse> respuesta = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), IcecatResponse.class);

            IcecatResponse cuerpo = respuesta.getBody();
            if (cuerpo == null || !"OK".equalsIgnoreCase(cuerpo.getMsg()) || cuerpo.getData() == null) {
                throw new IcecatApiException("Icecat no tiene ficha para el EAN " + ean);
            }
            return cuerpo;
        } catch (RestClientException ex) {
            throw new IcecatApiException(
                    "Error al consultar Icecat para el EAN " + ean + ": " + ex.getMessage());
        }
    }
}