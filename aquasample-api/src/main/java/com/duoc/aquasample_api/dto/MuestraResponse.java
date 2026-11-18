package com.duoc.aquasample_api.dto;

import com.duoc.aquasample_api.model.Chorito;
import com.duoc.aquasample_api.model.EstadoRevision;
import com.duoc.aquasample_api.model.Muestra;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo que devuelve la API. Incluye el conteo y el tamaño promedio ya calculados. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MuestraResponse {

    private Long id;
    private String uuid;
    private String centro;
    private String tren;
    private Integer linea;
    private Double longitudTramoM;
    private LocalDateTime fechaHora;
    private String operador;
    private String observaciones;
    private EstadoRevision estado;
    private String fotoNombre;
    private Integer cantidadChoritos;
    private Double tamanoPromedioCm;
    private List<ChoritoResponse> choritos;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public static MuestraResponse desde(Muestra muestra) {
        List<Double> tamanos = muestra.getChoritos().stream()
                .map(Chorito::getTamanoCm)
                .filter(Objects::nonNull)
                .toList();
        Double promedio = null;
        if (!tamanos.isEmpty()) {
            promedio = tamanos.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        return new MuestraResponse(
                muestra.getId(),
                muestra.getUuid(),
                muestra.getCentro(),
                muestra.getTren(),
                muestra.getLinea(),
                muestra.getLongitudTramoM(),
                muestra.getFechaHora(),
                muestra.getOperador(),
                muestra.getObservaciones(),
                muestra.getEstado(),
                muestra.getFotoNombre(),
                muestra.getChoritos().size(),
                promedio,
                muestra.getChoritos().stream().map(ChoritoResponse::desde).toList(),
                muestra.getCreadoEn(),
                muestra.getActualizadoEn()
        );
    }
}
