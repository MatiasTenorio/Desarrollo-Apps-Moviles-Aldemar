package com.duoc.aquasample_api.controller;

import com.duoc.aquasample_api.dto.RegistroUsuarioRequest;
import com.duoc.aquasample_api.dto.RolRequest;
import com.duoc.aquasample_api.dto.UsuarioResponse;
import com.duoc.aquasample_api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Usuarios que iniciaron sesión con Insforge, con su rol en AquaSample")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @Operation(
            summary = "Registrar acceso",
            description = "La app lo llama tras iniciar sesión en Insforge. Crea al usuario como OPERADOR "
                    + "la primera vez y devuelve su nombre y rol."
    )
    public ResponseEntity<?> registrarAcceso(@Valid @RequestBody RegistroUsuarioRequest request) {
        UsuarioResponse usuario = usuarioService.registrarAcceso(request.getCorreo());
        return ResponseEntity.status(HttpStatus.OK).body(usuario);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios")
    public ResponseEntity<?> listar() {
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.listar());
    }

    @PatchMapping("/{id}/rol")
    @Operation(summary = "Cambiar el rol (OPERADOR o ANALISTA)")
    public ResponseEntity<?> cambiarRol(@PathVariable Long id, @Valid @RequestBody RolRequest request) {
        Optional<UsuarioResponse> usuario = usuarioService.cambiarRol(id, request.getRol());
        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe el usuario con id " + id);
        }
        return ResponseEntity.status(HttpStatus.OK).body(usuario.get());
    }
}
