package com.duoc.aquasample_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Foto de una muestra, identificada por el uuid de la muestra (una foto por muestra). */
@Entity
@Table(name = "foto_muestra")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoMuestra {

    @Id
    @Column(length = 36)
    private String uuid;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] contenido;

    @Column(name = "tipo_contenido", nullable = false, length = 50)
    private String tipoContenido;

    @Column(name = "tamano_bytes", nullable = false)
    private Integer tamanoBytes;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
