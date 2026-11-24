package com.duoc.aquasample_api.dto;

import com.duoc.aquasample_api.model.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cambios que hace el Administrador sobre un usuario existente. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarUsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @NotNull(message = "El rol es obligatorio (OPERADOR, ANALISTA o ADMINISTRADOR)")
    private Rol rol;
}
