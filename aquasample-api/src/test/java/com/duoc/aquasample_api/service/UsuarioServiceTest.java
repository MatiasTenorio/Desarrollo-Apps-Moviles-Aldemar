package com.duoc.aquasample_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.duoc.aquasample_api.dto.UsuarioResponse;
import com.duoc.aquasample_api.model.Rol;
import com.duoc.aquasample_api.model.Usuario;
import com.duoc.aquasample_api.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioService usuarioService;

    private final ZoneId zona = ZoneId.of("America/Santiago");
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), zona);

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, reloj);
    }

    private void saveDevuelveLaEntidad() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> {
            Usuario usuario = invocacion.getArgument(0);
            if (usuario.getId() == null) {
                usuario.setId(1L);
            }
            return usuario;
        });
    }

    @Test
    void elPrimerAccesoCreaUnOperadorConElNombreDelCorreo() {
        when(usuarioRepository.findByCorreo("operador1@test.cl")).thenReturn(Optional.empty());
        saveDevuelveLaEntidad();

        UsuarioResponse usuario = usuarioService.registrarAcceso("  Operador1@Test.cl ");

        assertThat(usuario.getId()).isEqualTo(1L);
        assertThat(usuario.getCorreo()).isEqualTo("operador1@test.cl");
        assertThat(usuario.getNombre()).isEqualTo("operador1");
        assertThat(usuario.getRol()).isEqualTo(Rol.OPERADOR);
        assertThat(usuario.getUltimoAcceso()).isEqualTo(LocalDateTime.now(reloj));
    }

    @Test
    void unAccesoPosteriorConservaNombreYRol() {
        Usuario existente = new Usuario(5L, "analista1@test.cl", "Analista1", Rol.ANALISTA, null, null);
        when(usuarioRepository.findByCorreo("analista1@test.cl")).thenReturn(Optional.of(existente));
        saveDevuelveLaEntidad();

        UsuarioResponse usuario = usuarioService.registrarAcceso("analista1@test.cl");

        assertThat(usuario.getId()).isEqualTo(5L);
        assertThat(usuario.getNombre()).isEqualTo("Analista1");
        assertThat(usuario.getRol()).isEqualTo(Rol.ANALISTA);
        assertThat(usuario.getUltimoAcceso()).isEqualTo(LocalDateTime.now(reloj));
    }

    @Test
    void listar() {
        when(usuarioRepository.findAll()).thenReturn(
                List.of(new Usuario(1L, "a@test.cl", "a", Rol.OPERADOR, null, null)));

        assertThat(usuarioService.listar()).extracting(UsuarioResponse::getCorreo).containsExactly("a@test.cl");
    }

    @Test
    void cambiarRol() {
        when(usuarioRepository.findById(1L)).thenReturn(
                Optional.of(new Usuario(1L, "a@test.cl", "a", Rol.OPERADOR, null, null)));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        saveDevuelveLaEntidad();

        assertThat(usuarioService.cambiarRol(1L, Rol.ANALISTA)).get()
                .extracting(UsuarioResponse::getRol).isEqualTo(Rol.ANALISTA);
        assertThat(usuarioService.cambiarRol(99L, Rol.ANALISTA)).isEmpty();
    }

    @Test
    void elConstructorDeSpringUsaElRelojDelSistema() {
        when(usuarioRepository.findByCorreo(any())).thenReturn(Optional.empty());
        saveDevuelveLaEntidad();

        UsuarioResponse usuario = new UsuarioService(usuarioRepository).registrarAcceso("x@test.cl");

        assertThat(usuario.getUltimoAcceso()).isNotNull();
    }
}
