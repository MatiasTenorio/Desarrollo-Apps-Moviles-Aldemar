package com.duoc.aquasample_api.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.duoc.aquasample_api.service.UsuarioService;
import org.junit.jupiter.api.Test;

class AdministradorInicialTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);

    @Test
    void conCorreoConfiguradoAseguraAlAdministrador() {
        new AdministradorInicial(usuarioService, "jefa@test.cl").run(null);

        verify(usuarioService).asegurarAdministrador("jefa@test.cl");
    }

    @Test
    void sinCorreoNoHaceNada() {
        new AdministradorInicial(usuarioService, "").run(null);
        new AdministradorInicial(usuarioService, null).run(null);
        new AdministradorInicial(usuarioService, "sin-arroba").run(null);

        verifyNoInteractions(usuarioService);
    }
}
