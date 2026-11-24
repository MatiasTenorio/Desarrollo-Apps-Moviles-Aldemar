package com.duoc.aquasample_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.duoc.aquasample_api.config.InsforgeAdmin;
import com.duoc.aquasample_api.config.InsforgeAdmin.UsuarioInsforge;
import com.duoc.aquasample_api.config.VerificadorDominio;
import com.duoc.aquasample_api.config.VerificadorDominio.EstadoDominio;
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

    @Mock
    private InsforgeAdmin insforge;

    @Mock
    private VerificadorDominio verificadorDominio;

    private UsuarioService usuarioService;

    private final ZoneId zona = ZoneId.of("America/Santiago");
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), zona);

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, insforge, verificadorDominio, reloj);
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

    @Test
    void crearDaDeAltaConNombreYRol() {
        when(usuarioRepository.findByCorreo("ana@test.cl")).thenReturn(Optional.empty());
        saveDevuelveLaEntidad();

        UsuarioResponse usuario = usuarioService.crear(" Ana@Test.cl ", "  Ana Pérez ", Rol.ANALISTA);

        assertThat(usuario.getCorreo()).isEqualTo("ana@test.cl");
        assertThat(usuario.getNombre()).isEqualTo("Ana Pérez");
        assertThat(usuario.getRol()).isEqualTo(Rol.ANALISTA);
        assertThat(usuario.getUltimoAcceso()).isEqualTo(LocalDateTime.now(reloj));
    }

    @Test
    void crearUnCorreoExistenteActualizaNombreYRol() {
        Usuario existente = new Usuario(5L, "ana@test.cl", "ana", Rol.OPERADOR, null, LocalDateTime.now(reloj));
        when(usuarioRepository.findByCorreo("ana@test.cl")).thenReturn(Optional.of(existente));
        saveDevuelveLaEntidad();

        UsuarioResponse usuario = usuarioService.crear("ana@test.cl", "Ana Pérez", Rol.ADMINISTRADOR);

        assertThat(usuario.getId()).isEqualTo(5L);
        assertThat(usuario.getNombre()).isEqualTo("Ana Pérez");
        assertThat(usuario.getRol()).isEqualTo(Rol.ADMINISTRADOR);
    }

    @Test
    void actualizarCambiaNombreYRol() {
        Usuario existente = new Usuario(5L, "ana@test.cl", "ana", Rol.OPERADOR, null, LocalDateTime.now(reloj));
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        saveDevuelveLaEntidad();

        Optional<UsuarioResponse> usuario = usuarioService.actualizar(5L, " Ana ", Rol.ANALISTA);

        assertThat(usuario).isPresent();
        assertThat(usuario.get().getNombre()).isEqualTo("Ana");
        assertThat(usuario.get().getRol()).isEqualTo(Rol.ANALISTA);
    }

    @Test
    void actualizarUnUsuarioInexistenteDevuelveVacio() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(usuarioService.actualizar(9L, "X", Rol.OPERADOR)).isEmpty();
    }

    @Test
    void asegurarAdministradorCreaAlUsuarioSiNoExiste() {
        when(usuarioRepository.findByCorreo("jefa@test.cl")).thenReturn(Optional.empty());
        saveDevuelveLaEntidad();

        UsuarioResponse usuario = usuarioService.asegurarAdministrador(" Jefa@Test.cl ");

        assertThat(usuario.getNombre()).isEqualTo("jefa");
        assertThat(usuario.getRol()).isEqualTo(Rol.ADMINISTRADOR);
    }

    @Test
    void asegurarAdministradorConservaElNombre() {
        Usuario existente = new Usuario(5L, "jefa@test.cl", "Jefa de planta", Rol.ANALISTA, null, LocalDateTime.now(reloj));
        when(usuarioRepository.findByCorreo("jefa@test.cl")).thenReturn(Optional.of(existente));
        saveDevuelveLaEntidad();

        UsuarioResponse usuario = usuarioService.asegurarAdministrador("jefa@test.cl");

        assertThat(usuario.getNombre()).isEqualTo("Jefa de planta");
        assertThat(usuario.getRol()).isEqualTo(Rol.ADMINISTRADOR);
    }

    // --- Sincronización con Insforge ---

    private Usuario local(Long id, String correo) {
        return new Usuario(id, correo, correo.substring(0, correo.indexOf('@')), Rol.OPERADOR, null, LocalDateTime.now(reloj));
    }

    @Test
    void listarAgregaLasCuentasNuevasDeInsforgeYQuitaLasQueYaNoEstan() {
        Usuario sigue = local(1L, "b@test.cl");
        Usuario borrada = local(2L, "c@test.cl");
        when(insforge.configurado()).thenReturn(true);
        when(insforge.listarUsuarios()).thenReturn(List.of(
                new UsuarioInsforge("id-a", "a@test.cl", "Ana Test"),
                new UsuarioInsforge("id-b", "b@test.cl", null),
                new UsuarioInsforge("id-d", "d@test.cl", null)));
        when(usuarioRepository.findAll()).thenReturn(List.of(sigue, borrada));

        usuarioService.listar();

        verify(usuarioRepository).save(argThat((Usuario u) -> u.getCorreo().equals("a@test.cl")
                && u.getNombre().equals("Ana Test") && u.getRol() == Rol.OPERADOR));
        verify(usuarioRepository).save(argThat((Usuario u) -> u.getCorreo().equals("d@test.cl") && u.getNombre().equals("d")));
        verify(usuarioRepository).deleteAll(List.of(borrada));
    }

    @Test
    void siInsforgeFallaNoSeBorraNada() {
        when(insforge.configurado()).thenReturn(true);
        when(insforge.listarUsuarios()).thenThrow(new IllegalStateException("sin red"));
        when(usuarioRepository.findAll()).thenReturn(List.of(local(1L, "a@test.cl")));

        assertThat(usuarioService.listar()).hasSize(1);
        verify(usuarioRepository, never()).deleteAll(anyList());
    }

    @Test
    void unaListaVaciaDeInsforgeNoBorraANadie() {
        when(insforge.configurado()).thenReturn(true);
        when(insforge.listarUsuarios()).thenReturn(List.of());
        when(usuarioRepository.findAll()).thenReturn(List.of(local(1L, "a@test.cl")));

        assertThat(usuarioService.listar()).hasSize(1);
        verify(usuarioRepository, never()).deleteAll(anyList());
    }

    @Test
    void sinCambiosNoSeBorraNiSeCrea() {
        when(insforge.configurado()).thenReturn(true);
        when(insforge.listarUsuarios()).thenReturn(List.of(new UsuarioInsforge("id-a", "a@test.cl", "A")));
        when(usuarioRepository.findAll()).thenReturn(List.of(local(1L, "a@test.cl")));

        usuarioService.listar();

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(usuarioRepository, never()).deleteAll(anyList());
    }

    @Test
    void unNombreMuyLargoDeInsforgeSeRecorta() {
        when(insforge.configurado()).thenReturn(true);
        when(insforge.listarUsuarios()).thenReturn(List.of(new UsuarioInsforge("id", "a@test.cl", "x".repeat(120))));
        when(usuarioRepository.findAll()).thenReturn(List.of());

        usuarioService.listar();

        verify(usuarioRepository).save(argThat((Usuario u) -> u.getNombre().length() == 100));
    }

    @Test
    void darDeBajaEliminaEnInsforgeYEnMysql() {
        Usuario ana = local(5L, "a@test.cl");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(ana));
        when(insforge.configurado()).thenReturn(true);
        when(insforge.listarUsuarios()).thenReturn(List.of(
                new UsuarioInsforge("id-a", "a@test.cl", null),
                new UsuarioInsforge("id-b", "b@test.cl", null)));

        assertThat(usuarioService.eliminar(5L)).isTrue();

        verify(insforge).eliminarUsuarios(List.of("id-a"));
        verify(usuarioRepository).delete(ana);
    }

    @Test
    void darDeBajaSinCuentaEnInsforgeSoloBorraEnMysql() {
        Usuario ana = local(5L, "a@test.cl");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(ana));
        when(insforge.configurado()).thenReturn(true);
        when(insforge.listarUsuarios()).thenReturn(List.of(new UsuarioInsforge("id-b", "b@test.cl", null)));

        assertThat(usuarioService.eliminar(5L)).isTrue();

        verify(insforge, never()).eliminarUsuarios(anyList());
        verify(usuarioRepository).delete(ana);
    }

    @Test
    void darDeBajaSinInsforgeConfiguradoBorraSoloEnMysql() {
        Usuario ana = local(5L, "a@test.cl");
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(ana));

        assertThat(usuarioService.eliminar(5L)).isTrue();

        verify(usuarioRepository).delete(ana);
    }

    @Test
    void darDeBajaUnIdInexistenteDevuelveFalse() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(usuarioService.eliminar(9L)).isFalse();
    }

    // --- Validación del correo ---

    @Test
    void unCorreoConDominioQueRecibeCorreoSeAcepta() {
        when(verificadorDominio.verificar("gmail.com")).thenReturn(EstadoDominio.RECIBE_CORREO);
        assertThat(usuarioService.problemaConCorreo(" Ana@Gmail.com ")).isEmpty();
    }

    @Test
    void unDominioInexistenteOSinCorreoSeRechaza() {
        when(verificadorDominio.verificar("gmial.com")).thenReturn(EstadoDominio.NO_EXISTE);
        when(verificadorDominio.verificar("sincorreo.cl")).thenReturn(EstadoDominio.SIN_CORREO);

        assertThat(usuarioService.problemaConCorreo("ana@gmial.com"))
                .contains("El dominio gmial.com no existe. Revisa el correo.");
        assertThat(usuarioService.problemaConCorreo("ana@sincorreo.cl"))
                .contains("El dominio sincorreo.cl no recibe correos. Revisa el correo.");
    }

    @Test
    void sinDnsNoSeBloqueaElAlta() {
        when(verificadorDominio.verificar("duocuc.cl")).thenReturn(EstadoDominio.NO_SE_PUDO_VERIFICAR);
        assertThat(usuarioService.problemaConCorreo("ana@duocuc.cl")).isEmpty();
    }
}
