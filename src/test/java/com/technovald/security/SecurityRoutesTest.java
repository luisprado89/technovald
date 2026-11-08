package com.technovald.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void adminPuedeListarCategorias() throws Exception {
        mockMvc.perform(get("/api/admin/categorias")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void trabajadorPuedeListarCategorias() throws Exception {
        mockMvc.perform(get("/api/admin/categorias")
                        .with(user("trabajador").roles("TRABAJADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void trabajadorNoPuedeCrearCategoria() throws Exception {
        mockMvc.perform(post("/api/admin/categorias")
                        .with(user("trabajador").roles("TRABAJADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nueva\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeListarProveedores() throws Exception {
        mockMvc.perform(get("/api/admin/proveedores")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeVerInventario() throws Exception {
        mockMvc.perform(get("/api/admin/inventario")
                        .with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void trabajadorPuedeVerStockBajo() throws Exception {
        mockMvc.perform(get("/api/admin/inventario/low-stock")
                        .with(user("trabajador").roles("TRABAJADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void adminPuedeListarClientes() throws Exception {
        mockMvc.perform(get("/api/admin/clientes")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void trabajadorPuedeListarClientes() throws Exception {
        mockMvc.perform(get("/api/admin/clientes")
                        .with(user("trabajador").roles("TRABAJADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeListarEmpleados() throws Exception {
        mockMvc.perform(get("/api/admin/empleados")
                        .with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPuedeListarEmpleados() throws Exception {
        mockMvc.perform(get("/api/admin/empleados")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void elCatalogoPublicoNoRequiereToken() throws Exception {
        mockMvc.perform(get("/api/catalogo"))
                .andExpect(status().isOk());
    }
}
