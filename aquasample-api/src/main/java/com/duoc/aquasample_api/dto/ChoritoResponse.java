package com.duoc.aquasample_api.dto;

import com.duoc.aquasample_api.model.Chorito;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChoritoResponse {

    private Integer numero;
    private Double posX;
    private Double posY;
    private Double tamanoCm;

    public static ChoritoResponse desde(Chorito chorito) {
        return new ChoritoResponse(chorito.getNumero(), chorito.getPosX(), chorito.getPosY(), chorito.getTamanoCm());
    }
}
