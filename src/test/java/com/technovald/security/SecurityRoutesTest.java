package com.technovald.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRoutesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminPuedeConsultarEstadisticas() throws Exception {
        mockMvc.perform(get("/api/stats/productos-mas-vendidos")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeConsultarEstadisticas() throws Exception {
        mockMvc.perform(get("/api/stats/productos-mas-vendidos")
                        .with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinAutenticacionDevuelve401() throws Exception {
        mockMvc.perform(get("/api/stats/productos-mas-vendidos"))
                .andExpect(status().isUnauthorized());
    }
}
