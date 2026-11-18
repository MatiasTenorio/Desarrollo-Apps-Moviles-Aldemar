package com.duoc.aquasample_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.aquasample_api.TestDatos;
import com.duoc.aquasample_api.dto.ChoritoRequest;
import com.duoc.aquasample_api.dto.MuestraRequest;
import com.duoc.aquasample_api.dto.MuestraResponse;
import com.duoc.aquasample_api.model.EstadoRevision;
import com.duoc.aquasample_api.model.Muestra;
import com.duoc.aquasample_api.repository.MuestraRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MuestraServiceTest {

    @Mock
    private MuestraRepository muestraRepository;

    private MuestraService muestraService;

    @BeforeEach
    void setUp() {
        muestraService = new MuestraService(muestraRepository);
    }

    /** save devuelve la misma entidad, con id 1 si era nueva (como haría la BD). */
    private void saveDevuelveLaEntidad() {
        when(muestraRepository.save(any(Muestra.class))).thenAnswer(invocacion -> {
            Muestra muestra = invocacion.getArgument(0);
            if (muestra.getId() == null) {
                muestra.setId(1L);
            }
            return muestra;
        });
    }

    @Test
    void guardarPorUuidCreaLaMuestraSiNoExiste() {
        when(muestraRepository.findByUuid(TestDatos.UUID)).thenReturn(Optional.empty());
        saveDevuelveLaEntidad();

        ResultadoGuardado resultado = muestraService.guardarPorUuid(TestDatos.request());

        assertThat(resultado.creada()).isTrue();
        MuestraResponse muestra = resultado.muestra();
        assertThat(muestra.getId()).isEqualTo(1L);
        assertThat(muestra.getUuid()).isEqualTo(TestDatos.UUID);
        assertThat(muestra.getCentro()).isEqualTo("Centro Bahía Norte");
        assertThat(muestra.getObservaciones()).isEqualTo("Sin novedades");
        assertThat(muestra.getEstado()).isEqualTo(EstadoRevision.PENDIENTE);
        assertThat(muestra.getCantidadChoritos()).isEqualTo(2);
    }

    @Test
    void guardarPorUuidActualizaSiYaExisteSinDuplicar() {
        Muestra existente = TestDatos.muestra(5L);
        when(muestraRepository.findByUuid(TestDatos.UUID)).thenReturn(Optional.of(existente));
        saveDevuelveLaEntidad();
        MuestraRequest request = TestDatos.request();
        request.setLinea(4);
        request.getChoritos().remove(1);

        ResultadoGuardado resultado = muestraService.guardarPorUuid(request);

        assertThat(resultado.creada()).isFalse();
        assertThat(resultado.muestra().getId()).isEqualTo(5L);
        assertThat(resultado.muestra().getLinea()).isEqualTo(4);
        assertThat(resultado.muestra().getCantidadChoritos()).isEqualTo(1);
    }

    @Test
    void observacionesNulasSeGuardanVacias() {
        when(muestraRepository.findByUuid(TestDatos.UUID)).thenReturn(Optional.empty());
        saveDevuelveLaEntidad();
        MuestraRequest request = TestDatos.request();
        request.setObservaciones(null);
        request.setChoritos(null);

        MuestraResponse muestra = muestraService.guardarPorUuid(request).muestra();

        assertThat(muestra.getObservaciones()).isEmpty();
        assertThat(muestra.getCantidadChoritos()).isZero();
    }

    @Test
    void noSePuedeValidarUnaMuestraSinChoritos() {
        MuestraRequest request = TestDatos.request();
        request.setEstado(EstadoRevision.VALIDADO);
        request.setChoritos(new ArrayList<>());

        assertThatThrownBy(() -> muestraService.guardarPorUuid(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No se puede validar una muestra sin choritos contados");
        verify(muestraRepository, never()).save(any(Muestra.class));
    }

    @Test
    void noSeAceptanNumerosDeChoritoRepetidos() {
        MuestraRequest request = TestDatos.request();
        request.getChoritos().add(new ChoritoRequest(1, 0.1, 0.1, null));

        assertThatThrownBy(() -> muestraService.guardarPorUuid(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repetido");
    }

    @Test
    void listarConvierteElPeriodoIncluyendoElDiaHasta() {
        when(muestraRepository.buscar(any(), any(), any(), any(), any())).thenReturn(List.of(TestDatos.muestra(7L)));

        List<MuestraResponse> muestras = muestraService.listar(
                "Centro Bahía Norte", null, 2, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(muestras).hasSize(1);
        verify(muestraRepository).buscar(
                "Centro Bahía Norte", null, 2,
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 12, 1, 0, 0));
    }

    @Test
    void listarSinFiltrosPasaNulos() {
        when(muestraRepository.buscar(null, null, null, null, null)).thenReturn(List.of());

        assertThat(muestraService.listar(null, null, null, null, null)).isEmpty();
    }

    @Test
    void buscarPorId() {
        when(muestraRepository.findById(7L)).thenReturn(Optional.of(TestDatos.muestra(7L)));
        when(muestraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(muestraService.buscarPorId(7L)).isPresent();
        assertThat(muestraService.buscarPorId(99L)).isEmpty();
    }

    @Test
    void actualizarConservaElUuidYDevuelveVacioSiNoExiste() {
        when(muestraRepository.findById(5L)).thenReturn(Optional.of(TestDatos.muestra(5L)));
        when(muestraRepository.findById(99L)).thenReturn(Optional.empty());
        saveDevuelveLaEntidad();
        MuestraRequest request = TestDatos.request();
        request.setUuid("otro-uuid");
        request.setTren("Tren 3");

        Optional<MuestraResponse> actualizada = muestraService.actualizar(5L, request);

        assertThat(actualizada).isPresent();
        assertThat(actualizada.get().getTren()).isEqualTo("Tren 3");
        assertThat(actualizada.get().getUuid()).isEqualTo(TestDatos.UUID);
        assertThat(muestraService.actualizar(99L, request)).isEmpty();
    }

    @Test
    void cambiarEstado() {
        when(muestraRepository.findById(5L)).thenReturn(Optional.of(TestDatos.muestra(5L)));
        when(muestraRepository.findById(99L)).thenReturn(Optional.empty());
        saveDevuelveLaEntidad();

        Optional<MuestraResponse> validada = muestraService.cambiarEstado(5L, EstadoRevision.VALIDADO);

        assertThat(validada).isPresent();
        assertThat(validada.get().getEstado()).isEqualTo(EstadoRevision.VALIDADO);
        assertThat(muestraService.cambiarEstado(99L, EstadoRevision.OBSERVADO)).isEmpty();
    }

    @Test
    void cambiarEstadoAValidadoSinChoritosFalla() {
        Muestra vacia = TestDatos.muestra(5L);
        vacia.setChoritos(new ArrayList<>());
        when(muestraRepository.findById(5L)).thenReturn(Optional.of(vacia));

        assertThatThrownBy(() -> muestraService.cambiarEstado(5L, EstadoRevision.VALIDADO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eliminar() {
        when(muestraRepository.existsById(5L)).thenReturn(true);
        when(muestraRepository.existsById(99L)).thenReturn(false);

        assertThat(muestraService.eliminar(5L)).isTrue();
        assertThat(muestraService.eliminar(99L)).isFalse();
        verify(muestraRepository).deleteById(5L);
        verify(muestraRepository, never()).deleteById(99L);
    }

    @Test
    void losChoritosSeReemplazanAlActualizar() {
        Muestra existente = TestDatos.muestra(5L);
        when(muestraRepository.findByUuid(TestDatos.UUID)).thenReturn(Optional.of(existente));
        saveDevuelveLaEntidad();
        MuestraRequest request = TestDatos.request();
        request.setChoritos(new ArrayList<>(List.of(new ChoritoRequest(1, 0.9, 0.9, 7.0))));

        muestraService.guardarPorUuid(request);

        ArgumentCaptor<Muestra> guardada = ArgumentCaptor.forClass(Muestra.class);
        verify(muestraRepository).save(guardada.capture());
        assertThat(guardada.getValue().getChoritos()).hasSize(1);
        assertThat(guardada.getValue().getChoritos().get(0).getTamanoCm()).isEqualTo(7.0);
    }
}
