package com.duoc.aquasample_api.repository;

import com.duoc.aquasample_api.model.Muestra;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MuestraRepository extends JpaRepository<Muestra, Long> {

    Optional<Muestra> findByUuid(String uuid);

    /** Mismos filtros que el Historial de la app. Un parámetro null no filtra. "hasta" es exclusivo. */
    @Query("""
            SELECT m FROM Muestra m
            WHERE (:centro IS NULL OR m.centro = :centro)
              AND (:tren IS NULL OR m.tren = :tren)
              AND (:linea IS NULL OR m.linea = :linea)
              AND (:desde IS NULL OR m.fechaHora >= :desde)
              AND (:hasta IS NULL OR m.fechaHora < :hasta)
            ORDER BY m.fechaHora DESC
            """)
    List<Muestra> buscar(
            @Param("centro") String centro,
            @Param("tren") String tren,
            @Param("linea") Integer linea,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
