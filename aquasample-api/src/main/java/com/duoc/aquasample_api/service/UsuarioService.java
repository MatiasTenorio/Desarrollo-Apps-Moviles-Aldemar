package com.duoc.aquasample_api.service;

import com.duoc.aquasample_api.dto.UsuarioResponse;
import com.duoc.aquasample_api.model.Rol;
import com.duoc.aquasample_api.model.Usuario;
import com.duoc.aquasample_api.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final Clock reloj;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this(usuarioRepository, Clock.systemDefaultZone());
    }

    /** Con reloj inyectable, para que las pruebas controlen la hora. */
    UsuarioService(UsuarioRepository usuarioRepository, Clock reloj) {
        this.usuarioRepository = usuarioRepository;
        this.reloj = reloj;
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

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::desde).toList();
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
