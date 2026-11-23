package com.duoc.aquasample_api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.aquasample_api.dto.UsuarioResponse;
import com.duoc.aquasample_api.model.Rol;
import com.duoc.aquasample_api.service.UsuarioService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    private final UsuarioResponse analista = new UsuarioResponse(2L, "analista1@test.cl", "Analista1", Rol.ANALISTA, null);

    @Test
    void registrarAccesoDevuelveNombreYRol() throws Exception {
        when(usuarioService.registrarAcceso("analista1@test.cl")).thenReturn(analista);

        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"correo\": \"analista1@test.cl\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Analista1"))
                .andExpect(jsonPath("$.rol").value("ANALISTA"));
    }

    @Test
    void unCorreoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"correo\": \"no-es-correo\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("correo: El correo no es válido"));
    }

    @Test
    void listar() throws Exception {
        when(usuarioService.listar()).thenReturn(List.of(analista));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("analista1@test.cl"));
    }

    @Test
    void cambiarRolO404() throws Exception {
        when(usuarioService.cambiarRol(2L, Rol.OPERADOR)).thenReturn(Optional.of(analista));
        when(usuarioService.cambiarRol(99L, Rol.OPERADOR)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/usuarios/2/rol").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"rol\": \"OPERADOR\" }"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/usuarios/99/rol").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"rol\": \"OPERADOR\" }"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No existe el usuario con id 99"));
    }
}
