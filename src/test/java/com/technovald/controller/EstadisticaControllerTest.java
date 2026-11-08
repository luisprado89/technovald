package com.technovald.controller;

import com.technovald.dto.response.ProductoMasVendidoResponse;
import com.technovald.service.EstadisticaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EstadisticaControllerTest {

    private EstadisticaService estadisticaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        estadisticaService = mock(EstadisticaService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EstadisticaController(estadisticaService))
                .build();
    }

    @Test
    void devuelveElTopEnJson() throws Exception {
        when(estadisticaService.getProductosMasVendidos()).thenReturn(List.of(
                ProductoMasVendidoResponse.builder()
                        .nombreProducto("Teclado Mod")
                        .marca("Marca")
                        .cantidadTotalVendida(2L)
                        .build()));

        mockMvc.perform(get("/api/stats/productos-mas-vendidos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0].nombreProducto").value("Teclado Mod"))
                .andExpect(jsonPath("$[0].marca").value("Marca"))
                .andExpect(jsonPath("$[0].cantidadTotalVendida").value(2));
    }
}
