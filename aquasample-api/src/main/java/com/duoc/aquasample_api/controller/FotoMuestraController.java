package com.duoc.aquasample_api.controller;

import com.duoc.aquasample_api.model.FotoMuestra;
import com.duoc.aquasample_api.service.FotoMuestraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/muestras/{uuid}/foto")
@Tag(name = "Fotos", description = "Foto de cada muestra guardada en el servidor (respaldo si se cambia de teléfono)")
public class FotoMuestraController {

    private final FotoMuestraService fotoService;

    public FotoMuestraController(FotoMuestraService fotoService) {
        this.fotoService = fotoService;
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir o reemplazar la foto", description = "Campo multipart \"foto\". Máximo 10 MB.")
    public ResponseEntity<?> subir(@PathVariable String uuid, @RequestParam("foto") MultipartFile foto) throws IOException {
        if (!fotoService.guardar(uuid, foto.getBytes(), foto.getContentType())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe la muestra " + uuid);
        }
        return ResponseEntity.status(HttpStatus.OK).body("Foto guardada");
    }

    @GetMapping
    @Operation(summary = "Descargar la foto")
    public ResponseEntity<?> descargar(@PathVariable String uuid) {
        Optional<FotoMuestra> foto = fotoService.obtener(uuid);
        if (foto.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La muestra no tiene foto en el servidor");
        }
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(foto.get().getTipoContenido()))
                .body(foto.get().getContenido());
    }
}
