# AquaSample API

Backend Spring Boot de AquaSample (ALDEMAR SpA): CRUD de muestras de choritos y
sincronización de las muestras guardadas offline en la app.

- Spring Boot 4.1 · Java 21 · MySQL 8.4 · Flyway · Swagger (springdoc 3.2)

## Levantar en local

1. Crear el archivo `.env` (no se sube a git) con las credenciales de la base local,
   usando `.env.example` como guía.
2. Levantar MySQL con Docker:
   ```
   docker compose up -d
   ```
3. Iniciar la API:
   ```
   .\mvnw.cmd spring-boot:run
   ```
   Flyway crea las tablas al iniciar.
4. Documentación y pruebas manuales: http://localhost:8080/swagger-ui.html

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/muestras` | Lista. Filtros opcionales: `centro`, `tren`, `linea`, `desde`, `hasta` (AAAA-MM-DD) |
| GET | `/api/muestras/{id}` | Una muestra con sus choritos |
| POST | `/api/muestras` | Crea (201) o, si el `uuid` ya existe, actualiza (200). Lo usa la sincronización de la app |
| PUT | `/api/muestras/{id}` | Actualiza |
| PATCH | `/api/muestras/{id}/estado` | Cambia el estado: `PENDIENTE`, `OBSERVADO` o `VALIDADO` |
| DELETE | `/api/muestras/{id}` | Elimina la muestra y sus choritos |
| POST | `/api/usuarios` | La app lo llama tras iniciar sesión en Insforge: registra al usuario (rol OPERADOR la primera vez) y devuelve nombre y rol |
| GET | `/api/usuarios` | Lista los usuarios |
| PATCH | `/api/usuarios/{id}/rol` | Cambia el rol: `OPERADOR` o `ANALISTA` |

Los errores se responden con texto plano y código 400 o 404.

## Pruebas

```
.\mvnw.cmd test
```
No necesitan base de datos: el servicio se prueba con Mockito y los endpoints con MockMvc.
