package com.duoc.aquasample_api.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Muestra de choritos registrada desde la app.
 *
 * uuid: identificador que genera el teléfono al crear la muestra. Sirve para
 * la sincronización offline: si el teléfono reenvía la misma muestra (por
 * ejemplo porque se cortó la red antes de recibir la respuesta), el backend
 * la reconoce por su uuid y la actualiza en vez de duplicarla.
 *
 * Los choritos pertenecen a la muestra (se guardan y borran junto con ella),
 * por eso aquí sí se usa una relación JPA y no un id suelto.
 */
@Entity
@Table(name = "muestra")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Muestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(nullable = false, length = 100)
    private String centro;

    @Column(nullable = false, length = 50)
    private String tren;

    @Column(nullable = false)
    private Integer linea;

    @Column(name = "longitud_tramo_m", nullable = false)
    private Double longitudTramoM;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 100)
    private String operador;

    @Column(nullable = false, length = 500)
    private String observaciones = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRevision estado = EstadoRevision.PENDIENTE;

    @Column(name = "foto_nombre")
    private String fotoNombre;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "muestra_id", nullable = false)
    @OrderBy("numero ASC")
    private List<Chorito> choritos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
