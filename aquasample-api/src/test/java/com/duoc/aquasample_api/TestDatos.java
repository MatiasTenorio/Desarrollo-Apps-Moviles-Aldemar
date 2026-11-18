package com.duoc.aquasample_api;

import com.duoc.aquasample_api.dto.ChoritoRequest;
import com.duoc.aquasample_api.dto.MuestraRequest;
import com.duoc.aquasample_api.model.Chorito;
import com.duoc.aquasample_api.model.EstadoRevision;
import com.duoc.aquasample_api.model.Muestra;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Datos de prueba compartidos. */
public final class TestDatos {

    public static final String UUID = "6f1c2a9e-3b7d-4c55-9e1a-2d8f0b4c7a11";
    public static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 24, 9, 15);

    private TestDatos() {
    }

    public static MuestraRequest request() {
        List<ChoritoRequest> choritos = new ArrayList<>();
        choritos.add(new ChoritoRequest(1, 0.25, 0.40, 5.0));
        choritos.add(new ChoritoRequest(2, 0.60, 0.70, 6.0));
        return new MuestraRequest(UUID, " Centro Bahía Norte ", "Tren 1", 2, 1.5, FECHA,
                "operador1", " Sin novedades ", null, "muestra_1.jpg", choritos);
    }

    public static Muestra muestra(Long id) {
        List<Chorito> choritos = new ArrayList<>();
        choritos.add(new Chorito(10L, 1, 0.25, 0.40, 5.0));
        choritos.add(new Chorito(11L, 2, 0.60, 0.70, 6.0));
        return new Muestra(id, UUID, "Centro Bahía Norte", "Tren 1", 2, 1.5, FECHA, "operador1",
                "Sin novedades", EstadoRevision.PENDIENTE, "muestra_1.jpg", choritos, FECHA, FECHA);
    }
}
