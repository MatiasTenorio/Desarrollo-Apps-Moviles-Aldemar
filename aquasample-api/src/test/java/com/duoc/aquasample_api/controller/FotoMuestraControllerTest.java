package com.duoc.aquasample_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.aquasample_api.model.FotoMuestra;
import com.duoc.aquasample_api.service.FotoMuestraService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FotoMuestraController.class)
class FotoMuestraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FotoMuestraService fotoService;

    private final byte[] jpg = {1, 2, 3};
    private final MockMultipartFile archivo = new MockMultipartFile("foto", "muestra.jpg", "image/jpeg", jpg);

    @Test
    void subirLaFotoDevuelve200() throws Exception {
        when(fotoService.guardar(eq("u1"), any(), eq("image/jpeg"))).thenReturn(true);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/muestras/u1/foto").file(archivo))
                .andExpect(status().isOk())
                .andExpect(content().string("Foto guardada"));
    }

    @Test
    void subirLaFotoDeUnaMuestraInexistenteDevuelve404() throws Exception {
        when(fotoService.guardar(eq("nada"), any(), any())).thenReturn(false);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/muestras/nada/foto").file(archivo))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No existe la muestra nada"));
    }

    @Test
    void unArchivoQueNoEsImagenDevuelve400() throws Exception {
        when(fotoService.guardar(eq("u1"), any(), any())).thenThrow(new IllegalArgumentException("El archivo debe ser una imagen"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/muestras/u1/foto").file(archivo))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El archivo debe ser una imagen"));
    }

    @Test
    void descargarDevuelveLaImagenConSuTipo() throws Exception {
        when(fotoService.obtener("u1")).thenReturn(Optional.of(new FotoMuestra("u1", jpg, "image/jpeg", 3, null)));

        mockMvc.perform(get("/api/muestras/u1/foto"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(jpg));
    }

    @Test
    void descargarSinFotoDevuelve404() throws Exception {
        when(fotoService.obtener("u2")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/muestras/u2/foto"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("La muestra no tiene foto en el servidor"));
    }
}
