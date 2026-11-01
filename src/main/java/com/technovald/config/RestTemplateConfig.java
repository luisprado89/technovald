package com.technovald.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración del bean {@link RestTemplate} utilizado por
 * {@code IcecatClient} para consumir la API de Icecat.
 *
 * <p>
 * Define timeouts de conexión y lectura para evitar que la
 * aplicación se quede colgada si Icecat no responde.
 * </p>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Crea el bean RestTemplate con timeouts configurados.
     *
     * @return RestTemplate listo para ser inyectado en IcecatClient.
     */
    @Bean
    public RestTemplate restTemplate() {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        return new RestTemplate(factory);
    }
}