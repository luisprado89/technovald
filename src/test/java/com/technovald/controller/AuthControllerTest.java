package com.technovald.controller;

import com.technovald.dto.request.ForgotPasswordRequest;
import com.technovald.dto.request.LoginRequest;
import com.technovald.dto.response.AuthResponse;
import com.technovald.exception.GlobalExceptionHandler;
import com.technovald.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginDevuelveElTokenYLosDatosDelUsuario() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(AuthResponse.builder()
                .token("jwt-token")
                .username("ana")
                .rol("ADMIN")
                .expiracion(LocalDateTime.now().plusHours(1))
                .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"old\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("ana"))
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void loginRechazaCredencialesVaciasCon400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("username")));
    }

    @Test
    void forgotPasswordResponde204SinRevelarSiExiste() throws Exception {
        doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@test.local\"}"))
                .andExpect(status().isNoContent());

        verify(authService).forgotPassword(any(ForgotPasswordRequest.class));
    }
}
