package com.duoc.aquasample_api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.web.client.RestClientException;

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

    @Test
    void altaDeUsuarioDevuelve201() throws Exception {
        UsuarioResponse nuevo = new UsuarioResponse(3L, "ana@test.cl", "Ana Pérez", Rol.ANALISTA, null);
        when(usuarioService.crear("ana@test.cl", "Ana Pérez", Rol.ANALISTA)).thenReturn(nuevo);

        mockMvc.perform(post("/api/usuarios/alta").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"correo\": \"ana@test.cl\", \"nombre\": \"Ana Pérez\", \"rol\": \"ANALISTA\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.nombre").value("Ana Pérez"))
                .andExpect(jsonPath("$.rol").value("ANALISTA"));
    }

    @Test
    void altaSinNombreDevuelve400() throws Exception {
        mockMvc.perform(post("/api/usuarios/alta").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"correo\": \"ana@test.cl\", \"nombre\": \" \", \"rol\": \"ANALISTA\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarCambiaNombreYRol() throws Exception {
        UsuarioResponse admin = new UsuarioResponse(2L, "analista1@test.cl", "Jefa", Rol.ADMINISTRADOR, null);
        when(usuarioService.actualizar(2L, "Jefa", Rol.ADMINISTRADOR)).thenReturn(Optional.of(admin));

        mockMvc.perform(patch("/api/usuarios/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"Jefa\", \"rol\": \"ADMINISTRADOR\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    void actualizarUnUsuarioInexistenteDevuelve404() throws Exception {
        when(usuarioService.actualizar(99L, "X", Rol.OPERADOR)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/usuarios/99").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"nombre\": \"X\", \"rol\": \"OPERADOR\" }"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No existe el usuario con id 99"));
    }

    @Test
    void darDeBajaDevuelve204() throws Exception {
        when(usuarioService.eliminar(3L)).thenReturn(true);

        mockMvc.perform(delete("/api/usuarios/3")).andExpect(status().isNoContent());
    }

    @Test
    void darDeBajaUnIdInexistenteDevuelve404() throws Exception {
        when(usuarioService.eliminar(9L)).thenReturn(false);

        mockMvc.perform(delete("/api/usuarios/9"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No existe el usuario con id 9"));
    }

    @Test
    void siInsforgeFallaAlDarDeBajaDevuelve502() throws Exception {
        when(usuarioService.eliminar(3L)).thenThrow(new RestClientException("403"));

        mockMvc.perform(delete("/api/usuarios/3"))
                .andExpect(status().isBadGateway())
                .andExpect(content().string("Insforge no permitió dar de baja la cuenta"));
    }

    @Test
    void validarUnCorreoAceptadoDevuelve200() throws Exception {
        when(usuarioService.problemaConCorreo("ana@gmail.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/usuarios/validar-correo").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"correo\": \"ana@gmail.com\" }"))
                .andExpect(status().isOk())
                .andExpect(content().string("Correo válido"));
    }

    @Test
    void validarUnCorreoRechazadoDevuelve400ConElMotivo() throws Exception {
        when(usuarioService.problemaConCorreo("ana@gmial.com"))
                .thenReturn(Optional.of("El dominio gmial.com no existe. Revisa el correo."));

        mockMvc.perform(post("/api/usuarios/validar-correo").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"correo\": \"ana@gmial.com\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El dominio gmial.com no existe. Revisa el correo."));
    }
}
