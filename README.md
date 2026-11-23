# AquaSample · ALDEMAR SpA

App Android para el **registro, conteo y trazabilidad de muestras de choritos** en centros de cultivo, con backend propio y funcionamiento offline.
Proyecto del ramo DSY1105 – Desarrollo de Aplicaciones Móviles (DuocUC). Todos los datos y fotos son ficticios.

## Funcionalidades

| | |
|---|---|
| **Login** | Con **Insforge** (correo y contraseña). Al entrar, el backend registra al usuario en MySQL con su rol (Operador o Analista) |
| **Nueva muestra** | Asistente de 3 pasos: centro, tren, línea, longitud del tramo y foto (CameraX o galería) → **conteo asistido** tocando cada chorito sobre la foto, con corrección manual y tamaño opcional → resumen, observaciones y guardado |
| **Historial** | Filtros por centro, tren, línea y período, búsqueda y resumen para comparar |
| **Detalle** | Todos los datos, foto con zoom (Telephoto) y **cambio de estado de revisión** (Pendiente → Observado → Validado) |
| **Dashboard** | Indicadores y gráficos: choritos por día, muestras por estado y promedio por línea |
| **Reporte PDF** | Exporta y comparte las muestras validadas (identificación, conteo, observaciones y fecha) |
| **Condiciones del mar** | Oleaje, temperatura del mar y corriente de referencia por centro (API externa Open-Meteo Marine) |
| **Offline** | Todo se guarda en Room; WorkManager sube las muestras pendientes al backend cuando hay red |

## Arquitectura

```
Desarrollo-Apps-Moviles-Aldemar/
├── AquaSample/       App Android (Kotlin, Jetpack Compose, Material 3, MVVM)
├── aquasample-api/   Backend Spring Boot (CRUD, MySQL, Flyway, Swagger)
└── docs/             Evidencias de clases
```

- **App:** MVVM con `StateFlow`; Room (persistencia), Retrofit (backend y Open-Meteo), WorkManager (sincronización), CameraX + Coil + Telephoto (fotos), Navigation Compose. Inyección de dependencias manual en `AppContainer`.
- **Backend:** Spring Boot 4.1 · Java 21 · MySQL · Flyway · springdoc-openapi (Swagger). Endpoints en `/api/muestras` (detalle en `aquasample-api/README.md`).
- **Sincronización:** cada muestra tiene un `uuid` generado en el teléfono; el backend crea o actualiza por `uuid`, así un reenvío nunca duplica.

## Cómo ejecutar

Requisitos: Android Studio, Java 21 (sirve el de Android Studio) y Laragon con MySQL 8.

**1. Backend**
```
cd aquasample-api
Set-Content .env "DB_USER=root`nDB_PASSWORD="
.\mvnw.cmd spring-boot:run
```
Flyway crea la base `aquasample` y sus tablas. Swagger: http://localhost:8080/swagger-ui.html

**2. App**
Agregar a `AquaSample/local.properties` (no se sube a git) los datos del proyecto Insforge. Se usa la **anon key** (dashboard de Insforge → *Install* → *API Keys*), nunca la API key `ik_...` de administrador:
```
insforge.baseUrl=https://tu-proyecto.us-east.insforge.app/
insforge.anonKey=pegar_aqui_la_anon_key
```
Abrir `AquaSample` en Android Studio y ejecutar en un emulador (usa `http://10.0.2.2:8080/` para llegar al backend del PC).
En un teléfono real, agregar también la IP del PC:
```
api.baseUrl=http://192.168.1.50:8080/
```

**3. Usuarios y roles**
Los usuarios se crean en el dashboard de Insforge. El primer inicio de sesión los registra en la tabla `usuario` de MySQL como Operador. Para hacer Analista a alguien, en HeidiSQL (Laragon):
```sql
UPDATE usuario SET rol = 'ANALISTA' WHERE correo = 'correo@ejemplo.cl';
```

## Pruebas y cobertura

| | Comando (PowerShell) | Resultado |
|---|---|---|
| App, unitarias (JUnit + MockK) | `.\gradlew.bat testDebugUnitTest` | `app/build/reports/tests/` |
| App, cobertura (Kover, mínimo 90%) | `.\gradlew.bat koverHtmlReportDebug koverVerifyDebug` | `app/build/reports/kover/htmlDebug/index.html` |
| App, interfaz (Compose UI Test, con emulador abierto) | `.\gradlew.bat connectedDebugAndroidTest` | `app/build/reports/androidTests/` |
| Backend (JUnit + Mockito + MockMvc, JaCoCo mínimo 90%) | `.\mvnw.cmd verify` | `aquasample-api/target/site/jacoco/index.html` |

Los comandos de la app se ejecutan desde `AquaSample/` y los del backend desde `aquasample-api/`.

## APK firmado

1. Crear el keystore **fuera del repositorio** (Android Studio → *Build → Generate Signed App Bundle or APK → Create new*), por ejemplo en `C:\Users\Usuario\keystores\aquasample.jks`.
2. Crear `AquaSample/keystore.properties` (está en `.gitignore`, nunca se sube):
   ```
   storeFile=C:/Users/Usuario/keystores/aquasample.jks
   storePassword=tu_clave
   keyAlias=aquasample
   keyPassword=tu_clave
   ```
3. Generar: `.\gradlew.bat assembleRelease` → `AquaSample/app/build/outputs/apk/release/app-release.apk`

## Seguridad

No hay credenciales en el repositorio: `.env` (backend), `local.properties` y `keystore.properties` (app) están en `.gitignore`. La app solo lleva la anon key pública de Insforge; las contraseñas viven en Insforge, nunca en MySQL. Open-Meteo Marine no requiere API key.

## Defensa

Guía de demostración (roles, Swagger → app, offline, pruebas) y ensayo del cambio en vivo: [`docs/guia-defensa.md`](docs/guia-defensa.md).

## Equipo y planificación

Equipo 12 · DuocUC. Planificación en Trello: *(agregar enlace al tablero)*.
