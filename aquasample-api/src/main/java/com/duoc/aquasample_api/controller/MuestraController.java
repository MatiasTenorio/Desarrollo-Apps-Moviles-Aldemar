package com.duoc.aquasample_api.controller;

import com.duoc.aquasample_api.dto.EstadoRequest;
import com.duoc.aquasample_api.dto.MuestraRequest;
import com.duoc.aquasample_api.dto.MuestraResponse;
import com.duoc.aquasample_api.service.MuestraService;
import com.duoc.aquasample_api.service.ResultadoGuardado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/muestras")
@Tag(name = "Muestras", description = "CRUD de muestras de choritos y sincronización desde la app")
public class MuestraController {

    private final MuestraService muestraService;

    public MuestraController(MuestraService muestraService) {
        this.muestraService = muestraService;
    }

    @GetMapping
    @Operation(summary = "Listar muestras", description = "Filtros opcionales por centro, tren, línea y período (desde/hasta, formato AAAA-MM-DD).")
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String centro,
            @RequestParam(required = false) String tren,
            @RequestParam(required = false) Integer linea,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        List<MuestraResponse> muestras = muestraService.listar(centro, tren, linea, desde, hasta);
        return ResponseEntity.status(HttpStatus.OK).body(muestras);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una muestra por id")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        Optional<MuestraResponse> muestra = muestraService.buscarPorId(id);
        if (muestra.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe la muestra con id " + id);
        }
        return ResponseEntity.status(HttpStatus.OK).body(muestra.get());
    }

    @PostMapping
    @Operation(
            summary = "Crear o sincronizar una muestra",
            description = "Si ya existe una muestra con el mismo uuid se actualiza (200); si no, se crea (201). "
                    + "Así la app puede reenviar muestras pendientes sin duplicarlas."
    )
    public ResponseEntity<?> guardar(@Valid @RequestBody MuestraRequest request) {
        ResultadoGuardado resultado = muestraService.guardarPorUuid(request);
        if (resultado.creada()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado.muestra());
        }
        return ResponseEntity.status(HttpStatus.OK).body(resultado.muestra());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una muestra por id")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody MuestraRequest request) {
        Optional<MuestraResponse> muestra = muestraService.actualizar(id, request);
        if (muestra.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe la muestra con id " + id);
        }
        return ResponseEntity.status(HttpStatus.OK).body(muestra.get());
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de revisión (PENDIENTE, OBSERVADO, VALIDADO)")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        Optional<MuestraResponse> muestra = muestraService.cambiarEstado(id, request.getEstado());
        if (muestra.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe la muestra con id " + id);
        }
        return ResponseEntity.status(HttpStatus.OK).body(muestra.get());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una muestra y sus choritos")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        Boolean eliminada = muestraService.eliminar(id);
        if (!eliminada) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe la muestra con id " + id);
        }
        return ResponseEntity.status(HttpStatus.OK).body("Muestra eliminada");
    }
}
