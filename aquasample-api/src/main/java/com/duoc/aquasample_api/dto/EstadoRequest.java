package com.duoc.aquasample_api.dto;

import com.duoc.aquasample_api.model.EstadoRevision;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoRequest {

    @NotNull(message = "El estado es obligatorio (PENDIENTE, OBSERVADO o VALIDADO)")
    private EstadoRevision estado;
}
