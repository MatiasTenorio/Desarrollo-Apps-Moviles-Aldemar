package com.duoc.aquasample_api.service;

import com.duoc.aquasample_api.config.InsforgeAdmin;
import com.duoc.aquasample_api.config.InsforgeAdmin.UsuarioInsforge;
import com.duoc.aquasample_api.config.VerificadorDominio;
import com.duoc.aquasample_api.dto.UsuarioResponse;
import com.duoc.aquasample_api.model.Rol;
import com.duoc.aquasample_api.model.Usuario;
import com.duoc.aquasample_api.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final InsforgeAdmin insforge;
    private final VerificadorDominio verificadorDominio;
    private final Clock reloj;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, InsforgeAdmin insforge,
                          VerificadorDominio verificadorDominio) {
        this(usuarioRepository, insforge, verificadorDominio, Clock.systemDefaultZone());
    }

    /** Con reloj inyectable, para que las pruebas controlen la hora. */
    UsuarioService(UsuarioRepository usuarioRepository, InsforgeAdmin insforge,
                   VerificadorDominio verificadorDominio, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.insforge = insforge;
        this.verificadorDominio = verificadorDominio;
        this.reloj = reloj;
    }

    /**
     * Antes de crear la cuenta: el dominio del correo debe existir y recibir
     * correos. Detecta dominios inventados o mal escritos ("gmial.com"), pero no
     * si el buzón existe: eso solo lo prueba la verificación por correo de Insforge.
     * Devuelve el motivo del rechazo, o vacío si el correo se puede usar.
     */
    public Optional<String> problemaConCorreo(String correo) {
        String limpio = correo.trim().toLowerCase();
        String dominio = limpio.substring(limpio.indexOf('@') + 1);
        return switch (verificadorDominio.verificar(dominio)) {
            case NO_EXISTE -> Optional.of("El dominio " + dominio + " no existe. Revisa el correo.");
            case SIN_CORREO -> Optional.of("El dominio " + dominio + " no recibe correos. Revisa el correo.");
            case RECIBE_CORREO, NO_SE_PUDO_VERIFICAR -> Optional.empty();
        };
    }

    /**
     * Se llama cada vez que alguien inicia sesión. La primera vez crea al
     * usuario como OPERADOR, con el nombre tomado del correo (lo anterior a la @);
     * las siguientes solo actualiza su último acceso y conserva nombre y rol.
     */
    @Transactional
    public UsuarioResponse registrarAcceso(String correo) {
        String limpio = correo.trim().toLowerCase();
        Optional<Usuario> existente = usuarioRepository.findByCorreo(limpio);
        Usuario usuario;
        if (existente.isEmpty()) {
            usuario = new Usuario();
            usuario.setCorreo(limpio);
            usuario.setNombre(limpio.substring(0, limpio.indexOf('@')));
            usuario.setRol(Rol.OPERADOR);
        } else {
            usuario = existente.get();
        }
        usuario.setUltimoAcceso(LocalDateTime.now(reloj));
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    /** Antes de listar se sincroniza con Insforge, que es donde viven las cuentas. */
    @Transactional
    public List<UsuarioResponse> listar() {
        sincronizarConInsforge();
        return usuarioRepository.findAll().stream().map(UsuarioResponse::desde).toList();
    }

    /**
     * Insforge manda en qué cuentas existen; MySQL guarda nombre y rol.
     * - Cuenta nueva en Insforge (creada a mano en su panel): se agrega como OPERADOR.
     * - Usuario de MySQL cuya cuenta ya no está en Insforge: se elimina.
     * Si Insforge no responde o devuelve una lista vacía (lo más probable es un
     * error de lectura), no se toca nada: nunca se borra a todos por una falla.
     */
    void sincronizarConInsforge() {
        if (!insforge.configurado()) {
            return;
        }
        List<UsuarioInsforge> remotos;
        try {
            remotos = insforge.listarUsuarios();
        } catch (RuntimeException e) {
            log.warn("No se pudo leer los usuarios de Insforge: {}", e.getMessage());
            return;
        }
        if (remotos.isEmpty()) {
            return;
        }
        Set<String> correosInsforge = remotos.stream().map(UsuarioInsforge::email).collect(Collectors.toSet());
        List<Usuario> locales = usuarioRepository.findAll();
        Set<String> correosLocales = locales.stream().map(Usuario::getCorreo).collect(Collectors.toSet());

        for (UsuarioInsforge remoto : remotos) {
            if (!correosLocales.contains(remoto.email())) {
                Usuario nuevo = new Usuario();
                nuevo.setCorreo(remoto.email());
                nuevo.setNombre(nombreInicial(remoto));
                nuevo.setRol(Rol.OPERADOR);
                nuevo.setUltimoAcceso(LocalDateTime.now(reloj));
                usuarioRepository.save(nuevo);
            }
        }
        List<Usuario> ausentes = locales.stream()
                .filter(usuario -> !correosInsforge.contains(usuario.getCorreo()))
                .toList();
        if (!ausentes.isEmpty()) {
            usuarioRepository.deleteAll(ausentes);
        }
    }

    /**
     * Dar de baja: borra la cuenta en Insforge (ya no puede iniciar sesión) y
     * su fila en MySQL. Sus muestras se conservan: guardan el correo como texto.
     * Devuelve false si el id no existe. Si Insforge falla, lanza la excepción
     * y no se borra nada en MySQL (la transacción se revierte).
     */
    @Transactional
    public boolean eliminar(Long id) {
        Optional<Usuario> existente = usuarioRepository.findById(id);
        if (existente.isEmpty()) {
            return false;
        }
        Usuario usuario = existente.get();
        if (insforge.configurado()) {
            List<String> ids = insforge.listarUsuarios().stream()
                    .filter(remoto -> remoto.email().equals(usuario.getCorreo()) && remoto.id() != null)
                    .map(UsuarioInsforge::id)
                    .toList();
            if (!ids.isEmpty()) {
                insforge.eliminarUsuarios(ids);
            }
        }
        usuarioRepository.delete(usuario);
        return true;
    }

    private static String nombreInicial(UsuarioInsforge remoto) {
        String nombre = remoto.name() != null ? remoto.name().trim() : remoto.email().substring(0, remoto.email().indexOf('@'));
        return nombre.length() > 100 ? nombre.substring(0, 100) : nombre;
    }

    /**
     * Alta desde la pantalla del Administrador (la app ya creó la cuenta en
     * Insforge). Si el correo ya existía, por ejemplo porque esa persona ya
     * inició sesión una vez, se actualizan su nombre y rol en vez de fallar.
     */
    @Transactional
    public UsuarioResponse crear(String correo, String nombre, Rol rol) {
        String limpio = correo.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByCorreo(limpio).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setCorreo(limpio);
            nuevo.setUltimoAcceso(LocalDateTime.now(reloj));
            return nuevo;
        });
        usuario.setNombre(nombre.trim());
        usuario.setRol(rol);
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    /** El Administrador corrige el nombre visible y el rol. */
    @Transactional
    public Optional<UsuarioResponse> actualizar(Long id, String nombre, Rol rol) {
        Optional<Usuario> existente = usuarioRepository.findById(id);
        if (existente.isEmpty()) {
            return Optional.empty();
        }
        Usuario usuario = existente.get();
        usuario.setNombre(nombre.trim());
        usuario.setRol(rol);
        return Optional.of(UsuarioResponse.desde(usuarioRepository.save(usuario)));
    }

    /**
     * Garantiza un primer Administrador (correo en ADMIN_CORREO del .env).
     * Sin esto nadie podría entrar a la pantalla de usuarios para nombrar al resto.
     * Conserva el nombre si ya existía.
     */
    @Transactional
    public UsuarioResponse asegurarAdministrador(String correo) {
        String limpio = correo.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByCorreo(limpio).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setCorreo(limpio);
            nuevo.setNombre(limpio.substring(0, limpio.indexOf('@')));
            nuevo.setUltimoAcceso(LocalDateTime.now(reloj));
            return nuevo;
        });
        usuario.setRol(Rol.ADMINISTRADOR);
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public Optional<UsuarioResponse> cambiarRol(Long id, Rol rol) {
        Optional<Usuario> existente = usuarioRepository.findById(id);
        if (existente.isEmpty()) {
            return Optional.empty();
        }
        Usuario usuario = existente.get();
        usuario.setRol(rol);
        return Optional.of(UsuarioResponse.desde(usuarioRepository.save(usuario)));
    }
}
