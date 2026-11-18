package com.duoc.aquasample_api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChoritoRequest {

    @NotNull(message = "El número del chorito es obligatorio")
    @Positive(message = "El número del chorito debe ser mayor a 0")
    private Integer numero;

    @DecimalMin(value = "0.0", message = "La marca debe estar dentro de la foto (posX entre 0 y 1)")
    @DecimalMax(value = "1.0", message = "La marca debe estar dentro de la foto (posX entre 0 y 1)")
    private Double posX;

    @DecimalMin(value = "0.0", message = "La marca debe estar dentro de la foto (posY entre 0 y 1)")
    @DecimalMax(value = "1.0", message = "La marca debe estar dentro de la foto (posY entre 0 y 1)")
    private Double posY;

    @Positive(message = "El tamaño del chorito debe ser mayor a 0")
    private Double tamanoCm;
}
