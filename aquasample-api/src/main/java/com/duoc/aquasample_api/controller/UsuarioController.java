package com.duoc.aquasample_api.controller;

import com.duoc.aquasample_api.dto.ActualizarUsuarioRequest;
import com.duoc.aquasample_api.dto.AltaUsuarioRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

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
    @Operation(
            summary = "Listar usuarios",
            description = "Si INSFORGE_URL e INSFORGE_API_KEY están en el .env, antes sincroniza con Insforge: "
                    + "agrega las cuentas nuevas y quita las que ya no existen."
    )
    public ResponseEntity<?> listar() {
        return ResponseEntity.status(HttpStatus.OK).body(usuarioService.listar());
    }

    @PostMapping("/validar-correo")
    @Operation(
            summary = "Validar correo antes del alta",
            description = "Revisa que el dominio exista y reciba correos (registros DNS MX). "
                    + "200 si se puede usar; 400 con el motivo si no."
    )
    public ResponseEntity<?> validarCorreo(@Valid @RequestBody RegistroUsuarioRequest request) {
        Optional<String> problema = usuarioService.problemaConCorreo(request.getCorreo());
        if (problema.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problema.get());
        }
        return ResponseEntity.status(HttpStatus.OK).body("Correo válido");
    }

    @PostMapping("/alta")
    @Operation(
            summary = "Alta de usuario (Administrador)",
            description = "La app crea primero la cuenta en Insforge y luego la registra aquí con nombre y rol. "
                    + "Si el correo ya existe, actualiza su nombre y rol."
    )
    public ResponseEntity<?> crear(@Valid @RequestBody AltaUsuarioRequest request) {
        UsuarioResponse usuario = usuarioService.crear(request.getCorreo(), request.getNombre(), request.getRol());
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Cambiar nombre y rol (Administrador)")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest request) {
        Optional<UsuarioResponse> usuario = usuarioService.actualizar(id, request.getNombre(), request.getRol());
        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe el usuario con id " + id);
        }
        return ResponseEntity.status(HttpStatus.OK).body(usuario.get());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja (Administrador)", description = "Elimina la cuenta en Insforge y el usuario en MySQL.")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            if (!usuarioService.eliminar(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe el usuario con id " + id);
            }
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Insforge no permitió dar de baja la cuenta");
        }
    }

    @PatchMapping("/{id}/rol")
    @Operation(summary = "Cambiar el rol (OPERADOR, ANALISTA o ADMINISTRADOR)")
    public ResponseEntity<?> cambiarRol(@PathVariable Long id, @Valid @RequestBody RolRequest request) {
        Optional<UsuarioResponse> usuario = usuarioService.cambiarRol(id, request.getRol());
        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe el usuario con id " + id);
        }
        return ResponseEntity.status(HttpStatus.OK).body(usuario.get());
    }
}
