package com.duoc.aquasample_api.service;

import com.duoc.aquasample_api.model.FotoMuestra;
import com.duoc.aquasample_api.repository.FotoMuestraRepository;
import com.duoc.aquasample_api.repository.MuestraRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FotoMuestraService {

    /** Mismo límite que spring.servlet.multipart.max-file-size. */
    static final int TAMANO_MAXIMO_BYTES = 10 * 1024 * 1024;

    private final MuestraRepository muestraRepository;
    private final FotoMuestraRepository fotoRepository;
    private final Clock reloj;

    @Autowired
    public FotoMuestraService(MuestraRepository muestraRepository, FotoMuestraRepository fotoRepository) {
        this(muestraRepository, fotoRepository, Clock.systemDefaultZone());
    }

    FotoMuestraService(MuestraRepository muestraRepository, FotoMuestraRepository fotoRepository, Clock reloj) {
        this.muestraRepository = muestraRepository;
        this.fotoRepository = fotoRepository;
        this.reloj = reloj;
    }

    /**
     * Guarda (o reemplaza) la foto de la muestra con ese uuid.
     * Devuelve false si la muestra no existe: primero se sube la muestra y después su foto.
     */
    @Transactional
    public boolean guardar(String uuid, byte[] contenido, String tipoContenido) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("La foto está vacía");
        }
        if (contenido.length > TAMANO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("La foto supera 10 MB");
        }
        if (tipoContenido == null || !tipoContenido.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }
        if (muestraRepository.findByUuid(uuid).isEmpty()) {
            return false;
        }
        fotoRepository.save(new FotoMuestra(uuid, contenido, tipoContenido, contenido.length, LocalDateTime.now(reloj)));
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<FotoMuestra> obtener(String uuid) {
        return fotoRepository.findById(uuid);
    }
}
