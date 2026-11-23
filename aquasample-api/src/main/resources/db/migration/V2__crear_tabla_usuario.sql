-- Usuarios que iniciaron sesión con Insforge. Las contraseñas viven solo en
-- Insforge; aquí se guarda el correo, el nombre visible y el rol en AquaSample.
-- El rol se cambia desde Laragon (HeidiSQL), por ejemplo:
--   UPDATE usuario SET rol = 'ANALISTA' WHERE correo = 'correo@ejemplo.cl';

CREATE TABLE usuario (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    correo         VARCHAR(150) NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    rol            VARCHAR(20)  NOT NULL DEFAULT 'OPERADOR',
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('OPERADOR', 'ANALISTA'))
);
