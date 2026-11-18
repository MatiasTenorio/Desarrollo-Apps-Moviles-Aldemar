package com.duoc.aquasample_api.service;

import com.duoc.aquasample_api.dto.ChoritoRequest;
import com.duoc.aquasample_api.dto.MuestraRequest;
import com.duoc.aquasample_api.dto.MuestraResponse;
import com.duoc.aquasample_api.model.Chorito;
import com.duoc.aquasample_api.model.EstadoRevision;
import com.duoc.aquasample_api.model.Muestra;
import com.duoc.aquasample_api.repository.MuestraRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MuestraService {

    private final MuestraRepository muestraRepository;

    public MuestraService(MuestraRepository muestraRepository) {
        this.muestraRepository = muestraRepository;
    }

    /** Lista con filtros opcionales. "hasta" incluye todo ese día. */
    @Transactional(readOnly = true)
    public List<MuestraResponse> listar(String centro, String tren, Integer linea, LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = null;
        if (desde != null) {
            inicio = desde.atStartOfDay();
        }
        LocalDateTime fin = null;
        if (hasta != null) {
            fin = hasta.plusDays(1).atStartOfDay();
        }
        return muestraRepository.buscar(centro, tren, linea, inicio, fin).stream()
                .map(MuestraResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<MuestraResponse> buscarPorId(Long id) {
        return muestraRepository.findById(id).map(MuestraResponse::desde);
    }

    /**
     * Crea la muestra o, si ya existe una con el mismo uuid, la actualiza.
     * Así la app puede reenviar una muestra sin miedo a duplicarla.
     */
    @Transactional
    public ResultadoGuardado guardarPorUuid(MuestraRequest request) {
        validarReglas(request.getEstado(), request.getChoritos());

        Optional<Muestra> existente = muestraRepository.findByUuid(request.getUuid());
        Muestra muestra;
        boolean creada;
        if (existente.isEmpty()) {
            muestra = new Muestra();
            muestra.setUuid(request.getUuid());
            creada = true;
        } else {
            muestra = existente.get();
            creada = false;
        }
        copiarDatos(request, muestra);
        Muestra guardada = muestraRepository.save(muestra);
        return new ResultadoGuardado(MuestraResponse.desde(guardada), creada);
    }

    /** Actualiza por id. El uuid no cambia (identifica a la muestra en el teléfono). */
    @Transactional
    public Optional<MuestraResponse> actualizar(Long id, MuestraRequest request) {
        Optional<Muestra> existente = muestraRepository.findById(id);
        if (existente.isEmpty()) {
            return Optional.empty();
        }
        validarReglas(request.getEstado(), request.getChoritos());
        Muestra muestra = existente.get();
        copiarDatos(request, muestra);
        return Optional.of(MuestraResponse.desde(muestraRepository.save(muestra)));
    }

    @Transactional
    public Optional<MuestraResponse> cambiarEstado(Long id, EstadoRevision estado) {
        Optional<Muestra> existente = muestraRepository.findById(id);
        if (existente.isEmpty()) {
            return Optional.empty();
        }
        Muestra muestra = existente.get();
        if (estado == EstadoRevision.VALIDADO && muestra.getChoritos().isEmpty()) {
            throw new IllegalArgumentException("No se puede validar una muestra sin choritos contados");
        }
        muestra.setEstado(estado);
        return Optional.of(MuestraResponse.desde(muestraRepository.save(muestra)));
    }

    @Transactional
    public Boolean eliminar(Long id) {
        if (!muestraRepository.existsById(id)) {
            return false;
        }
        muestraRepository.deleteById(id);
        return true;
    }

    /** Las mismas reglas de negocio que ValidadorMuestra en la app. */
    private void validarReglas(EstadoRevision estado, List<ChoritoRequest> choritos) {
        List<ChoritoRequest> lista = choritos;
        if (lista == null) {
            lista = List.of();
        }
        if (estado == EstadoRevision.VALIDADO && lista.isEmpty()) {
            throw new IllegalArgumentException("No se puede validar una muestra sin choritos contados");
        }
        Set<Integer> numeros = new HashSet<>();
        for (ChoritoRequest chorito : lista) {
            if (!numeros.add(chorito.getNumero())) {
                throw new IllegalArgumentException("Hay choritos con el número repetido: " + chorito.getNumero());
            }
        }
    }

    private void copiarDatos(MuestraRequest request, Muestra muestra) {
        muestra.setCentro(request.getCentro().trim());
        muestra.setTren(request.getTren().trim());
        muestra.setLinea(request.getLinea());
        muestra.setLongitudTramoM(request.getLongitudTramoM());
        muestra.setFechaHora(request.getFechaHora());
        muestra.setOperador(request.getOperador().trim());
        String observaciones = request.getObservaciones();
        if (observaciones == null) {
            observaciones = "";
        }
        muestra.setObservaciones(observaciones.trim());
        if (request.getEstado() != null) {
            muestra.setEstado(request.getEstado());
        }
        muestra.setFotoNombre(request.getFotoNombre());

        // Se reemplaza el contenido de la lista (no la lista misma) para que
        // orphanRemoval borre los choritos que ya no vienen.
        muestra.getChoritos().clear();
        if (request.getChoritos() != null) {
            for (ChoritoRequest c : request.getChoritos()) {
                muestra.getChoritos().add(new Chorito(null, c.getNumero(), c.getPosX(), c.getPosY(), c.getTamanoCm()));
            }
        }
    }
}
