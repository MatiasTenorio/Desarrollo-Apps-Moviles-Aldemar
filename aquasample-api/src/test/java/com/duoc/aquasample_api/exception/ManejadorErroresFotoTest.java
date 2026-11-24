package com.duoc.aquasample_api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class ManejadorErroresFotoTest {

    @Test
    void unaFotoMuyGrandeDevuelve413() {
        ResponseEntity<?> respuesta = new ManejadorErrores().fotoMuyGrande(new MaxUploadSizeExceededException(10));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(respuesta.getBody()).isEqualTo("La foto supera 10 MB");
    }
}
