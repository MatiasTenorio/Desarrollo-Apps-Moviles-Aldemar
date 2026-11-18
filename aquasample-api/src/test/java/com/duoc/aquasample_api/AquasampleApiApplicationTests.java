package com.duoc.aquasample_api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Antes era un @SpringBootTest "contextLoads", que necesita MySQL encendido
 * y hacía fallar "mvnw test" sin base de datos. Las pruebas reales de la API
 * están en MuestraServiceTest (Mockito) y MuestraControllerTest (MockMvc),
 * que no necesitan base de datos.
 */
class AquasampleApiApplicationTests {

    @Test
    void laClasePrincipalEsUnaAplicacionSpringBoot() {
        assertThat(AquasampleApiApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }
}
