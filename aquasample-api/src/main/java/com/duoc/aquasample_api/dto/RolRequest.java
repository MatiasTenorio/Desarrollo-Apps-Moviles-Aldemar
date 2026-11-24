package com.duoc.aquasample_api.dto;

import com.duoc.aquasample_api.model.Rol;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolRequest {

    @NotNull(message = "El rol es obligatorio (OPERADOR, ANALISTA o ADMINISTRADOR)")
    private Rol rol;
}
