package com.duoc.aquasample_api.dto;

import com.duoc.aquasample_api.model.Rol;
import com.duoc.aquasample_api.model.Usuario;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Long id;
    private String correo;
    private String nombre;
    private Rol rol;
    private LocalDateTime ultimoAcceso;

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getCorreo(), usuario.getNombre(),
                usuario.getRol(), usuario.getUltimoAcceso());
    }
}
