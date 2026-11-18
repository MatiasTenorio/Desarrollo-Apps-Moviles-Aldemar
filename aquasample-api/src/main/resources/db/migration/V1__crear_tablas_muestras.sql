-- AquaSample: muestras de choritos y los choritos contados en cada una.

CREATE TABLE muestra (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    uuid             VARCHAR(36)  NOT NULL,
    centro           VARCHAR(100) NOT NULL,
    tren             VARCHAR(50)  NOT NULL,
    linea            INT          NOT NULL,
    longitud_tramo_m DOUBLE       NOT NULL,
    fecha_hora       DATETIME     NOT NULL,
    operador         VARCHAR(100) NOT NULL,
    observaciones    VARCHAR(500) NOT NULL DEFAULT '',
    estado           VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    foto_nombre      VARCHAR(255) NULL,
    creado_en        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_muestra_uuid UNIQUE (uuid),
    CONSTRAINT ck_muestra_estado CHECK (estado IN ('PENDIENTE', 'OBSERVADO', 'VALIDADO'))
);

CREATE INDEX idx_muestra_filtros ON muestra (centro, tren, linea, fecha_hora);

CREATE TABLE chorito (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    muestra_id BIGINT NOT NULL,
    numero     INT    NOT NULL,
    pos_x      DOUBLE NULL,
    pos_y      DOUBLE NULL,
    tamano_cm  DOUBLE NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chorito_muestra FOREIGN KEY (muestra_id) REFERENCES muestra (id) ON DELETE CASCADE
);
