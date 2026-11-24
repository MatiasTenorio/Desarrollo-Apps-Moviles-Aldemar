-- Foto de cada muestra en la base de datos del servidor, para que no se pierda
-- si se cambia de teléfono o se desinstala la app. Va en una tabla aparte para
-- que listar muestras no cargue las imágenes. Al borrar la muestra se borra su foto.
CREATE TABLE foto_muestra (
    uuid            VARCHAR(36)  NOT NULL,
    contenido       LONGBLOB     NOT NULL,
    tipo_contenido  VARCHAR(50)  NOT NULL,
    tamano_bytes    INT          NOT NULL,
    actualizado_en  DATETIME     NOT NULL,
    PRIMARY KEY (uuid),
    CONSTRAINT fk_foto_muestra_uuid FOREIGN KEY (uuid) REFERENCES muestra (uuid) ON DELETE CASCADE
);
