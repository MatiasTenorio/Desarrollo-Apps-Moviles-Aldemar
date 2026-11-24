package com.duoc.aquasample_api.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente de la API de administración de Insforge (listar y eliminar usuarios).
 *
 * Usa la API key de administrador del proyecto, que solo vive en el .env del
 * backend (INSFORGE_API_KEY). Nunca va en la app: con ella se puede borrar
 * cualquier cuenta. Si no está configurada, el backend funciona igual que
 * antes, sin sincronizar con Insforge.
 */
@Component
public class InsforgeAdmin {

    static final int POR_PAGINA = 100;
    private static final int MAX_PAGINAS = 20;

    /** Cuenta de Insforge: id (para eliminarla), correo y nombre visible. */
    public record UsuarioInsforge(String id, String email, String name) {
    }

    private final RestClient cliente;

    @Autowired
    public InsforgeAdmin(@Value("${insforge.url:}") String url, @Value("${insforge.api-key:}") String apiKey) {
        this(RestClient.builder(), url, apiKey);
    }

    /** Con builder recibido, para que las pruebas lo conecten a un servidor simulado. */
    InsforgeAdmin(RestClient.Builder builder, String url, String apiKey) {
        if (url == null || url.isBlank() || apiKey == null || apiKey.isBlank()) {
            this.cliente = null;
        } else {
            this.cliente = builder
                    .baseUrl(url.trim().replaceAll("/+$", ""))
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .build();
        }
    }

    public boolean configurado() {
        return cliente != null;
    }

    /** Todas las cuentas, página por página. Los correos vienen en minúsculas. */
    public List<UsuarioInsforge> listarUsuarios() {
        exigirConfigurado();
        List<UsuarioInsforge> todos = new ArrayList<>();
        for (int pagina = 1; pagina <= MAX_PAGINAS; pagina++) {
            final int numero = pagina;
            Object cuerpo = cliente.get()
                    .uri(uri -> uri.path("/api/auth/users")
                            .queryParam("page", numero)
                            .queryParam("limit", POR_PAGINA)
                            .build())
                    .retrieve()
                    .body(Object.class);
            List<?> lista = extraerLista(cuerpo);
            for (Object elemento : lista) {
                if (elemento instanceof Map<?, ?> usuario) {
                    String email = texto(usuario.get("email"));
                    if (email != null) {
                        todos.add(new UsuarioInsforge(texto(usuario.get("id")), email.trim().toLowerCase(), nombreDe(usuario)));
                    }
                }
            }
            if (lista.size() < POR_PAGINA) {
                break;
            }
        }
        return todos;
    }

    public void eliminarUsuarios(List<String> ids) {
        exigirConfigurado();
        cliente.method(HttpMethod.DELETE)
                .uri("/api/auth/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("userIds", ids))
                .retrieve()
                .toBodilessEntity();
    }

    private void exigirConfigurado() {
        if (cliente == null) {
            throw new IllegalStateException("Insforge no está configurado (INSFORGE_URL / INSFORGE_API_KEY)");
        }
    }

    /**
     * La documentación dice { "data": [...], "pagination": {...} }; se acepta
     * también "users" o una lista directa, por si la versión del servidor cambia.
     */
    private static List<?> extraerLista(Object cuerpo) {
        if (cuerpo instanceof List<?> lista) {
            return lista;
        }
        if (cuerpo instanceof Map<?, ?> mapa) {
            for (String clave : List.of("data", "users")) {
                if (mapa.get(clave) instanceof List<?> lista) {
                    return lista;
                }
            }
        }
        return List.of();
    }

    private static String nombreDe(Map<?, ?> usuario) {
        String nombre = texto(usuario.get("name"));
        if (nombre == null && usuario.get("profile") instanceof Map<?, ?> perfil) {
            nombre = texto(perfil.get("name"));
        }
        return nombre;
    }

    private static String texto(Object valor) {
        return valor == null || valor.toString().isBlank() ? null : valor.toString();
    }
}
