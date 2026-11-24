package com.duoc.aquasample_api.dto;

import com.duoc.aquasample_api.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo que envía el Administrador al dar de alta un usuario (la contraseña va solo a Insforge). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AltaUsuarioRequest {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    private String correo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;

    @NotNull(message = "El rol es obligatorio (OPERADOR, ANALISTA o ADMINISTRADOR)")
    private Rol rol;
}
