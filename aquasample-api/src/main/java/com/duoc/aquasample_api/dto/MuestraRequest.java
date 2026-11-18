package com.duoc.aquasample_api.dto;

import com.duoc.aquasample_api.model.EstadoRevision;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo que envía la app para crear o actualizar una muestra. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MuestraRequest {

    @NotBlank(message = "El uuid es obligatorio")
    @Size(max = 36, message = "El uuid no puede superar 36 caracteres")
    private String uuid;

    @NotBlank(message = "El centro es obligatorio")
    @Size(max = 100, message = "El centro no puede superar 100 caracteres")
    private String centro;

    @NotBlank(message = "El tren es obligatorio")
    @Size(max = 50, message = "El tren no puede superar 50 caracteres")
    private String tren;

    @NotNull(message = "La línea es obligatoria")
    @Positive(message = "La línea debe ser mayor a 0")
    private Integer linea;

    @NotNull(message = "La longitud del tramo es obligatoria")
    @Positive(message = "La longitud del tramo debe ser mayor a 0")
    private Double longitudTramoM;

    @NotNull(message = "La fecha y hora son obligatorias")
    private LocalDateTime fechaHora;

    @NotBlank(message = "El operador es obligatorio")
    @Size(max = 100, message = "El operador no puede superar 100 caracteres")
    private String operador;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;

    /** Opcional: si no viene, la muestra queda PENDIENTE. */
    private EstadoRevision estado;

    private String fotoNombre;

    @Valid
    private List<ChoritoRequest> choritos = new ArrayList<>();
}
