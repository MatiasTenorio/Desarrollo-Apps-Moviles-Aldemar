package com.duoc.aquasample_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duoc.aquasample_api.TestDatos;
import com.duoc.aquasample_api.dto.MuestraResponse;
import com.duoc.aquasample_api.model.EstadoRevision;
import com.duoc.aquasample_api.service.MuestraService;
import com.duoc.aquasample_api.service.ResultadoGuardado;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Prueba solo la capa web (rutas, validación, códigos HTTP); el servicio es un mock. */
@WebMvcTest(MuestraController.class)
class MuestraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MuestraService muestraService;

    private static final String JSON_VALIDO = """
            {
              "uuid": "6f1c2a9e-3b7d-4c55-9e1a-2d8f0b4c7a11",
              "centro": "Centro Bahía Norte",
              "tren": "Tren 1",
              "linea": 2,
              "longitudTramoM": 1.5,
              "fechaHora": "2026-11-24T09:15:00",
              "operador": "operador1",
              "observaciones": "Sin novedades",
              "choritos": [
                { "numero": 1, "posX": 0.25, "posY": 0.40, "tamanoCm": 5.0 }
              ]
            }
            """;

    private MuestraResponse respuesta() {
        return MuestraResponse.desde(TestDatos.muestra(7L));
    }

    @Test
    void listarDevuelveLasMuestrasYPasaLosFiltros() throws Exception {
        when(muestraService.listar("Centro Bahía Norte", null, 2, LocalDate.of(2026, 11, 1), null))
                .thenReturn(List.of(respuesta()));

        mockMvc.perform(get("/api/muestras")
                        .param("centro", "Centro Bahía Norte")
                        .param("linea", "2")
                        .param("desde", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].cantidadChoritos").value(2))
                .andExpect(jsonPath("$[0].fechaHora").value("2026-11-24T09:15:00"));
    }

    @Test
    void buscarPorIdInexistenteDevuelve404ConTexto() throws Exception {
        when(muestraService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/muestras/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No existe la muestra con id 99"));
    }

    @Test
    void buscarPorIdExistente() throws Exception {
        when(muestraService.buscarPorId(7L)).thenReturn(Optional.of(respuesta()));

        mockMvc.perform(get("/api/muestras/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(TestDatos.UUID))
                .andExpect(jsonPath("$.tamanoPromedioCm").value(5.5));
    }

    @Test
    void postNuevaDevuelve201YExistenteDevuelve200() throws Exception {
        when(muestraService.guardarPorUuid(any())).thenReturn(new ResultadoGuardado(respuesta(), true));
        mockMvc.perform(post("/api/muestras").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));

        when(muestraService.guardarPorUuid(any())).thenReturn(new ResultadoGuardado(respuesta(), false));
        mockMvc.perform(post("/api/muestras").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk());
    }

    @Test
    void postInvalidoDevuelve400ConLosCamposConError() throws Exception {
        String sinCentroNiLinea = JSON_VALIDO
                .replace("\"centro\": \"Centro Bahía Norte\",", "\"centro\": \"\",")
                .replace("\"linea\": 2,", "\"linea\": 0,");

        mockMvc.perform(post("/api/muestras").contentType(MediaType.APPLICATION_JSON).content(sinCentroNiLinea))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("centro: El centro es obligatorio")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("linea: La línea debe ser mayor a 0")));
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/muestras").contentType(MediaType.APPLICATION_JSON).content("{ esto no es json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reglaDeNegocioDevuelve400ConElMensaje() throws Exception {
        when(muestraService.guardarPorUuid(any()))
                .thenThrow(new IllegalArgumentException("No se puede validar una muestra sin choritos contados"));

        mockMvc.perform(post("/api/muestras").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se puede validar una muestra sin choritos contados"));
    }

    @Test
    void putActualizaO404() throws Exception {
        when(muestraService.actualizar(eq(7L), any())).thenReturn(Optional.of(respuesta()));
        when(muestraService.actualizar(eq(99L), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/muestras/7").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/muestras/99").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiarEstado() throws Exception {
        MuestraResponse validada = respuesta();
        validada.setEstado(EstadoRevision.VALIDADO);
        when(muestraService.cambiarEstado(7L, EstadoRevision.VALIDADO)).thenReturn(Optional.of(validada));
        when(muestraService.cambiarEstado(99L, EstadoRevision.VALIDADO)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/muestras/7/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"estado\": \"VALIDADO\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDADO"));
        mockMvc.perform(patch("/api/muestras/99/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"estado\": \"VALIDADO\" }"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/muestras/7/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{ }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminar() throws Exception {
        when(muestraService.eliminar(7L)).thenReturn(true);
        when(muestraService.eliminar(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/muestras/7"))
                .andExpect(status().isOk())
                .andExpect(content().string("Muestra eliminada"));
        mockMvc.perform(delete("/api/muestras/99"))
                .andExpect(status().isNotFound());
    }
}
