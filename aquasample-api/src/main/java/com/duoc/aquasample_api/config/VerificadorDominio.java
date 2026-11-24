package com.duoc.aquasample_api.config;

/** Consulta si un dominio de correo existe y recibe correos (registros DNS). */
public interface VerificadorDominio {

    enum EstadoDominio {
        RECIBE_CORREO,
        SIN_CORREO,
        NO_EXISTE,
        /** Sin DNS disponible (sin internet, tiempo agotado): no se bloquea el alta por eso. */
        NO_SE_PUDO_VERIFICAR
    }

    EstadoDominio verificar(String dominio);
}
