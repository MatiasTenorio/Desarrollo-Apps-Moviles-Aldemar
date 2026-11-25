# AquaSample - ALDEMAR SpA

App Android para registrar, contar y revisar muestras de choritos en centros de cultivo. Funciona sin conexión y se sincroniza con un backend propio.

Proyecto del ramo DSY1105 - Desarrollo de Aplicaciones Móviles (DuocUC), Equipo 12. Los datos y fotos son ficticios.

## Estructura

```
AquaSample/       App Android (Kotlin, Jetpack Compose, Material 3, MVVM)
aquasample-api/   Backend (Spring Boot 4, Java 21, MySQL, Flyway, Swagger)
docs/             Evidencias de clase y guía de defensa
```

## Roles

| Rol | Qué hace |
|---|---|
| Operador | Registra muestras (foto, conteo, observaciones), las edita mientras no estén validadas y cambia su estado (Pendiente, Observado, Validado). |
| Analista | Consulta las muestras validadas de todos los operadores, las filtra por centro, tren, línea y operador, y exporta reportes PDF. No toma ni modifica muestras. |
| Administrador | Da de alta usuarios (en Insforge y en el backend), les asigna nombre y rol, los da de baja y puede eliminar muestras. Consulta el historial completo y exporta reportes. |

El rol se guarda en MySQL. Un usuario nuevo entra como Operador. El primer Administrador se define con `ADMIN_CORREO` en el `.env` del backend; los demás roles se asignan desde la pantalla Usuarios.

## Funcionalidades

- **Nueva muestra:** asistente de 3 pasos. Primero centro, tren, línea, longitud del tramo y foto (cámara o galería); luego el conteo tocando cada chorito sobre la foto, con tamaño opcional; al final, observaciones y guardado.
- **Historial:** búsqueda, filtros y resumen. Exporta a PDF lo que se está viendo.
- **Detalle:** foto con zoom, choritos medidos, cambio de estado, edición y eliminación, según el rol.
- **Dashboard:** indicadores y gráficos de choritos por día, muestras por estado y promedio por línea.
- **Condiciones del mar:** oleaje y temperatura del mar del centro elegido (Open-Meteo Marine).
- **Offline:** todo se guarda en el teléfono (Room). WorkManager sube y baja muestras y fotos cuando hay red.

## APIs

### Backend propio (`aquasample-api`, puerto 8080)

Swagger: `http://localhost:8080/swagger-ui.html`

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/muestras` | Listar (filtros opcionales: centro, tren, línea, período) |
| GET | `/api/muestras/{id}` | Una muestra |
| POST | `/api/muestras` | Crear o actualizar por `uuid` (201 o 200). Lo usa la sincronización |
| PUT | `/api/muestras/{id}` | Actualizar por id |
| PATCH | `/api/muestras/{id}/estado` | Cambiar estado de revisión |
| DELETE | `/api/muestras/{id}` | Eliminar muestra, choritos y foto |
| PUT | `/api/muestras/{uuid}/foto` | Subir la foto (multipart, campo `foto`, máximo 10 MB) |
| GET | `/api/muestras/{uuid}/foto` | Descargar la foto |
| POST | `/api/usuarios` | Registrar el acceso tras el login; devuelve nombre y rol |
| GET | `/api/usuarios` | Listar usuarios (antes se sincroniza con Insforge) |
| POST | `/api/usuarios/validar-correo` | Revisar que el dominio del correo exista y reciba correos |
| POST | `/api/usuarios/alta` | Alta desde la pantalla del Administrador |
| PATCH | `/api/usuarios/{id}` | Cambiar nombre y rol |
| PATCH | `/api/usuarios/{id}/rol` | Cambiar solo el rol |
| DELETE | `/api/usuarios/{id}` | Dar de baja (Insforge y MySQL) |

Los errores responden con texto plano en español: 400 (datos inválidos), 404 (no existe), 413 (foto muy grande), 502 (Insforge no respondió).

### Externas

- **Insforge:** autenticación. La app usa la anon key para iniciar sesión y crear cuentas. El backend usa la API key de administrador, solo desde su `.env`, para listar y eliminar cuentas.
- **Open-Meteo Marine:** `GET https://marine-api.open-meteo.com/v1/marine`, sin API key. Si falla la red o el servidor, la app muestra un mensaje y permite reintentar.

## Entidades

### Backend (MySQL)

| Tabla | Campos principales |
|---|---|
| `muestra` | id, uuid (único), centro, tren, linea, longitud_tramo_m, fecha_hora, operador (correo), observaciones, estado, foto_nombre |
| `chorito` | id, muestra_id, numero, pos_x, pos_y, tamano_cm |
| `foto_muestra` | uuid (de la muestra), contenido (imagen), tipo_contenido, tamano_bytes |
| `usuario` | id, correo (único), nombre, rol, ultimo_acceso |

`chorito` y `foto_muestra` se borran junto con su muestra. Las migraciones están en `aquasample-api/src/main/resources/db/migration` (V1 a V4).

### App (Room y modelos)

- **Muestra:** los mismos datos que en el backend, más `fotoPath` (ruta local de la foto), `estadoSync` (Pendiente, Sincronizado o Error) y `remoteId` (id en el backend).
- **Chorito:** número, posición relativa en la foto (0 a 1) y tamaño en cm (opcional).
- **Usuario:** nombre, correo y rol de la sesión actual.

Cada muestra tiene un `uuid` generado en el teléfono. El backend crea o actualiza por `uuid`, así un reenvío nunca duplica. Las muestras guardan el correo del operador, no su nombre: el nombre se busca al mostrarlas, porque puede cambiar.

## Configuración

**Backend:** archivo `aquasample-api/.env` (no se sube a git):

```
DB_USER=root
DB_PASSWORD=
ADMIN_CORREO=mtvj2701@gmail.com
ADMIN_PASS= 123456
INSFORGE_URL=https://insforge.dev/dashboard/project/424bb903-97ba-4888-8f67-d0a2ed476471
INSFORGE_API_KEY=key de administrador de Insforge
```

**App:** archivo `AquaSample/local.properties` (no se sube a git):

```
insforge.baseUrl=https://insforge.dev/dashboard/project/424bb903-97ba-4888-8f67-d0a2ed476471
insforge.anonKey=anon key de Insforge
api.baseUrl=http://localhost:8080/
```

`api.baseUrl` queda grabado en la APK al compilar:
- Emulador: `http://10.0.2.2:8080/` (el valor por defecto).
- Teléfono por USB: `http://localhost:8080/`, y ejecutar `adb reverse tcp:8080 tcp:8080`.
- Teléfono por Wi-Fi: la IP del PC, en la misma red y con el puerto 8080 abierto en el firewall.

## Ejecutar

1. Encender MySQL (Laragon).
2. Backend: en `aquasample-api`, ejecutar `.\mvnw.cmd spring-boot:run`. Flyway crea la base `aquasample` y sus tablas.
3. App: abrir `AquaSample` en Android Studio y ejecutar.

## Pruebas

| Qué | Comando | Reporte |
|---|---|---|
| App, unitarias (JUnit + MockK) | `.\gradlew.bat testDebugUnitTest` | `app/build/reports/tests/` |
| App, cobertura (Kover, mínimo 90%) | `.\gradlew.bat koverHtmlReportDebug koverVerifyDebug` | `app/build/reports/kover/htmlDebug/index.html` |
| App, interfaz (Compose UI Test, con teléfono o emulador) | `.\gradlew.bat connectedDebugAndroidTest` | `app/build/reports/androidTests/` |
| Backend (JUnit + Mockito + MockMvc, JaCoCo mínimo 90%) | `.\mvnw.cmd verify` | `target/site/jacoco/index.html` |

## APK firmada

1. Crear el keystore desde Android Studio (Build > Generate Signed App Bundle or APK > Create new).
2. Crear `AquaSample/keystore.properties` (no se sube a git) con `storeFile`, `storePassword`, `keyAlias` y `keyPassword`.
3. Ejecutar `.\gradlew.bat assembleRelease`. El resultado queda en `app/build/outputs/apk/release/app-release.apk`.

Versión actual: `versionCode 1`, `versionName 1.0.0`, en `AquaSample/app/build.gradle.kts`.

## Seguridad

- **Credenciales:** `.env`, `local.properties`, `keystore.properties` y los `.jks` están en `.gitignore`.
- **Insforge:** la app solo lleva la anon key; la API key de administrador queda únicamente en el backend. Las contraseñas viven en Insforge, nunca en MySQL.
- **Pendiente:** los endpoints del backend no exigen autenticación. Los permisos por rol se controlan en la app.

## Defensa

Guía de demostración: [`docs/guia-defensa.md`](docs/guia-defensa.md).