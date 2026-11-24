package com.duoc.aquasample_api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.duoc.aquasample_api.config.InsforgeAdmin.UsuarioInsforge;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class InsforgeAdminTest {

    private static final String URL = "https://demo.insforge.app";

    private MockRestServiceServer servidor;
    private InsforgeAdmin insforge;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        // Con barra final y espacios: se limpian.
        insforge = new InsforgeAdmin(builder, URL + "/ ", " clave-admin ");
    }

    private String pagina(int numero) {
        return URL + "/api/auth/users?page=" + numero + "&limit=" + InsforgeAdmin.POR_PAGINA;
    }

    @Test
    void listaUsuariosConLaApiKeyDeAdministrador() {
        servidor.expect(requestTo(pagina(1)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer clave-admin"))
                .andRespond(withSuccess("""
                        {"data": [
                          {"id": "u1", "email": "Ana@Test.cl", "name": "Ana"},
                          {"id": "u2", "email": "beto@test.cl", "profile": {"name": "Beto"}},
                          {"id": "u3", "name": "sin correo"}
                        ], "pagination": {"offset": 0, "limit": 100, "total": 2}}
                        """, MediaType.APPLICATION_JSON));

        List<UsuarioInsforge> usuarios = insforge.listarUsuarios();

        assertThat(insforge.configurado()).isTrue();
        assertThat(usuarios).containsExactly(
                new UsuarioInsforge("u1", "ana@test.cl", "Ana"),
                new UsuarioInsforge("u2", "beto@test.cl", "Beto"));
        servidor.verify();
    }

    @Test
    void recorreTodasLasPaginas() {
        String llena = IntStream.range(0, InsforgeAdmin.POR_PAGINA)
                .mapToObj(i -> "{\"id\": \"u" + i + "\", \"email\": \"u" + i + "@test.cl\"}")
                .collect(Collectors.joining(",", "{\"data\": [", "]}"));
        servidor.expect(requestTo(pagina(1))).andRespond(withSuccess(llena, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(pagina(2))).andRespond(withSuccess(
                "{\"users\": [{\"id\": \"x\", \"email\": \"ultimo@test.cl\", \"name\": \" \"}]}",
                MediaType.APPLICATION_JSON));

        List<UsuarioInsforge> usuarios = insforge.listarUsuarios();

        assertThat(usuarios).hasSize(InsforgeAdmin.POR_PAGINA + 1);
        assertThat(usuarios.get(InsforgeAdmin.POR_PAGINA)).isEqualTo(new UsuarioInsforge("x", "ultimo@test.cl", null));
        servidor.verify();
    }

    @Test
    void aceptaUnaListaDirectaYIgnoraFormatosDesconocidos() {
        servidor.expect(requestTo(pagina(1))).andRespond(withSuccess(
                "[{\"id\": \"u1\", \"email\": \"a@test.cl\"}, \"basura\"]", MediaType.APPLICATION_JSON));
        assertThat(insforge.listarUsuarios()).containsExactly(new UsuarioInsforge("u1", "a@test.cl", null));

        servidor.reset();
        servidor.expect(requestTo(pagina(1))).andRespond(withSuccess("{\"otra\": 1}", MediaType.APPLICATION_JSON));
        assertThat(insforge.listarUsuarios()).isEmpty();
    }

    @Test
    void eliminaUsuariosPorId() {
        servidor.expect(requestTo(URL + "/api/auth/users"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer clave-admin"))
                .andExpect(content().json("{\"userIds\": [\"u1\"]}"))
                .andRespond(withSuccess("{\"message\": \"ok\", \"deletedCount\": 1}", MediaType.APPLICATION_JSON));

        insforge.eliminarUsuarios(List.of("u1"));

        servidor.verify();
    }

    @Test
    void sinUrlOSinClaveNoEstaConfigurado() {
        InsforgeAdmin sinClave = new InsforgeAdmin("https://demo.insforge.app", " ");
        InsforgeAdmin sinUrl = new InsforgeAdmin(null, "clave");

        assertThat(sinClave.configurado()).isFalse();
        assertThat(sinUrl.configurado()).isFalse();
        assertThatThrownBy(sinClave::listarUsuarios).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> sinUrl.eliminarUsuarios(List.of("u1"))).isInstanceOf(IllegalStateException.class);
    }
}
