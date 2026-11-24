-- Nuevo rol ADMINISTRADOR: da de alta usuarios y les cambia nombre y rol desde la app.
ALTER TABLE usuario DROP CHECK ck_usuario_rol;
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_rol CHECK (rol IN ('OPERADOR', 'ANALISTA', 'ADMINISTRADOR'));
