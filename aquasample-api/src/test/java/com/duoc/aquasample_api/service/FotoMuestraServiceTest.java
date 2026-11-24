package com.duoc.aquasample_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.aquasample_api.model.FotoMuestra;
import com.duoc.aquasample_api.model.Muestra;
import com.duoc.aquasample_api.repository.FotoMuestraRepository;
import com.duoc.aquasample_api.repository.MuestraRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FotoMuestraServiceTest {

    @Mock
    private MuestraRepository muestraRepository;

    @Mock
    private FotoMuestraRepository fotoRepository;

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneId.of("America/Santiago"));
    private FotoMuestraService servicio;

    private final byte[] jpg = {1, 2, 3};

    @BeforeEach
    void setUp() {
        servicio = new FotoMuestraService(muestraRepository, fotoRepository, reloj);
    }

    @Test
    void guardaLaFotoDeUnaMuestraExistente() {
        when(muestraRepository.findByUuid("u1")).thenReturn(Optional.of(new Muestra()));

        assertThat(servicio.guardar("u1", jpg, "image/jpeg")).isTrue();

        ArgumentCaptor<FotoMuestra> guardada = ArgumentCaptor.forClass(FotoMuestra.class);
        verify(fotoRepository).save(guardada.capture());
        assertThat(guardada.getValue().getUuid()).isEqualTo("u1");
        assertThat(guardada.getValue().getContenido()).isEqualTo(jpg);
        assertThat(guardada.getValue().getTipoContenido()).isEqualTo("image/jpeg");
        assertThat(guardada.getValue().getTamanoBytes()).isEqualTo(3);
        assertThat(guardada.getValue().getActualizadoEn()).isEqualTo(LocalDateTime.now(reloj));
    }

    @Test
    void siLaMuestraNoExisteDevuelveFalse() {
        when(muestraRepository.findByUuid("nada")).thenReturn(Optional.empty());

        assertThat(servicio.guardar("nada", jpg, "image/png")).isFalse();
        verify(fotoRepository, never()).save(any());
    }

    @Test
    void rechazaFotosVaciasGrandesONoImagenes() {
        assertThatThrownBy(() -> servicio.guardar("u1", new byte[0], "image/jpeg"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("La foto está vacía");
        assertThatThrownBy(() -> servicio.guardar("u1", null, "image/jpeg"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.guardar("u1", new byte[FotoMuestraService.TAMANO_MAXIMO_BYTES + 1], "image/jpeg"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("La foto supera 10 MB");
        assertThatThrownBy(() -> servicio.guardar("u1", jpg, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("El archivo debe ser una imagen");
        assertThatThrownBy(() -> servicio.guardar("u1", jpg, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void obtieneLaFotoPorUuid() {
        FotoMuestra foto = new FotoMuestra("u1", jpg, "image/jpeg", 3, LocalDateTime.now(reloj));
        when(fotoRepository.findById("u1")).thenReturn(Optional.of(foto));

        assertThat(servicio.obtener("u1")).contains(foto);
    }
}
