package com.duoc.aquasample_api.config;

import com.duoc.aquasample_api.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, deja como ADMINISTRADOR el correo de ADMIN_CORREO (.env).
 * Así el primer Administrador no depende de un UPDATE a mano en la base de
 * datos, y el correo personal no queda escrito en el repositorio.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioService usuarioService;
    private final String correo;

    public AdministradorInicial(UsuarioService usuarioService,
                                @Value("${aquasample.admin-inicial:}") String correo) {
        this.usuarioService = usuarioService;
        this.correo = correo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (correo == null || correo.isBlank() || !correo.contains("@")) {
            return;
        }
        usuarioService.asegurarAdministrador(correo);
        log.info("Administrador inicial asegurado: {}", correo.trim().toLowerCase());
    }
}
