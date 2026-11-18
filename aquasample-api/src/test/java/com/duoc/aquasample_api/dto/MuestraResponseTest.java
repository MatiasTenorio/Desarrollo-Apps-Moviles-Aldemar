package com.duoc.aquasample_api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.duoc.aquasample_api.TestDatos;
import com.duoc.aquasample_api.model.Chorito;
import com.duoc.aquasample_api.model.Muestra;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MuestraResponseTest {

    @Test
    void calculaCantidadYTamanoPromedio() {
        MuestraResponse respuesta = MuestraResponse.desde(TestDatos.muestra(7L));

        assertThat(respuesta.getId()).isEqualTo(7L);
        assertThat(respuesta.getCantidadChoritos()).isEqualTo(2);
        assertThat(respuesta.getTamanoPromedioCm()).isEqualTo(5.5);
        assertThat(respuesta.getChoritos()).extracting(ChoritoResponse::getNumero).containsExactly(1, 2);
    }

    @Test
    void sinMedicionesElPromedioEsNull() {
        Muestra muestra = TestDatos.muestra(7L);
        List<Chorito> sinMedir = new ArrayList<>();
        sinMedir.add(new Chorito(1L, 1, 0.5, 0.5, null));
        muestra.setChoritos(sinMedir);

        MuestraResponse respuesta = MuestraResponse.desde(muestra);

        assertThat(respuesta.getCantidadChoritos()).isEqualTo(1);
        assertThat(respuesta.getTamanoPromedioCm()).isNull();
    }
}
