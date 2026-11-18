package com.duoc.aquasample_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un chorito contado en la foto. posX / posY son la marca del conteo
 * asistido como fracción del ancho/alto de la foto (0.0 a 1.0).
 */
@Entity
@Table(name = "chorito")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "pos_x")
    private Double posX;

    @Column(name = "pos_y")
    private Double posY;

    @Column(name = "tamano_cm")
    private Double tamanoCm;
}
