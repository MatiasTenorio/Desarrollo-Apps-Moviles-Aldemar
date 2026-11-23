# Guía para la defensa – AquaSample

Cómo mostrar los puntos que pide la pauta de la defensa, y el ensayo del **cambio en vivo**.

---

## 1. Roles y permisos

- **Operador**: registra muestras y las revisa y valida (Pendiente → Observado → Validado).
- **Analista**: solo consulta. No ve "Nueva muestra", y en Detalle los botones de estado aparecen deshabilitados.
- **¿Y el Supervisor?** El equipo fusionó Operador de muestreo y Supervisor técnico en un solo rol Operador (decisión documentada en la Evidencia de Clase 1): en ALDEMAR la misma persona en terreno registra y revisa la muestra.
- **Dónde está en el código:** `data/model/Permisos.kt` (reglas), usadas en `HomeViewModel`, `AquaSampleApp` (pestañas) y `SampleDetailViewModel`.
- **Dónde se guarda el rol:** tabla `usuario` de MySQL. Se cambia en HeidiSQL:
  ```sql
  UPDATE usuario SET rol = 'ANALISTA' WHERE correo = 'correo@ejemplo.cl';
  ```
  Después de cambiarlo, cerrar sesión y volver a entrar.

## 2. Swagger → app (Retrofit)

1. Backend corriendo y app abierta en el emulador, con sesión iniciada.
2. En http://localhost:8080/swagger-ui.html, ejecutar `PUT /api/muestras/{id}` o `POST /api/muestras`. Por ejemplo, cambiar las `observaciones` de una muestra existente.
3. En la app: **Historial → ícono de sincronizar** (flechas circulares, junto al filtro).
4. Abrir la muestra: se ve el dato nuevo.

Qué explicar: WorkManager sube las muestras pendientes (`POST`) y luego **baja todas las del servidor** (`GET /api/muestras`) a Room. Las identifica por su **uuid**, así que no duplica. Si una muestra tiene cambios locales sin subir, gana el teléfono. Las que se borran en el servidor también se borran en el teléfono.

> El número `#0007` de la app es el id local de Room; el `id` de Swagger es el del servidor. El dato que los une es el **uuid**, que se ve en Swagger.

## 3. Offline

1. Apagar el backend (Ctrl + C) o activar el modo avión en el emulador.
2. Crear una muestra: queda con la nube gris, "Pendiente de sincronizar".
3. Encender de nuevo y tocar "Sincronizar ahora" (o esperar a WorkManager): la nube queda verde.
4. Mostrar la fila nueva en HeidiSQL: `SELECT * FROM muestra ORDER BY id DESC;`

## 4. Pruebas en vivo

- Una prueba concreta: `ValidadorMuestraTest` → "la foto es obligatoria" (entrada: muestra sin foto → resultado: error "Agrega la foto de la muestra").
- Comandos (desde `AquaSample/`):
  ```
  .\gradlew.bat testDebugUnitTest
  .\gradlew.bat koverHtmlReportDebug
  .\gradlew.bat connectedDebugAndroidTest
  ```
- Compose UI Test con datos inválidos: `LoginScreenTest` (campos vacíos y contraseña incorrecta).

---

## 5. Cambio en vivo: "peso de la muestra (g)"

Objetivo de la pauta: menos de 5 minutos. **Hay que ensayarlo al menos 2 veces antes de la defensa.**

El cambio completo ya está hecho y probado como parche en `docs/ensayo-cambio-en-vivo-peso.patch`: sirve para comparar después de cada ensayo (`git diff`) y para ver el resultado esperado. **No lo apliquen en la defensa**; ahí se escribe a mano.

Todos los campos son opcionales (`null`), así nada existente se rompe.

### Backend (4 archivos + SQL)

**1. Migración nueva** `aquasample-api/src/main/resources/db/migration/V3__agregar_peso_muestra.sql`
```sql
ALTER TABLE muestra ADD COLUMN peso_g DOUBLE NULL;
```

**2. Entidad** `model/Muestra.java`, justo debajo de `private String fotoNombre;`
```java
@Column(name = "peso_g")
private Double pesoG;
```

**3. DTO de entrada** `dto/MuestraRequest.java`, debajo de `private String fotoNombre;`
```java
@Positive(message = "El peso debe ser mayor a 0")
private Double pesoG;
```

**4. DTO de salida** `dto/MuestraResponse.java`
- debajo de `private String fotoNombre;` → `private Double pesoG;`
- en `desde(...)`, debajo de `muestra.getFotoNombre(),` → `muestra.getPesoG(),`

**5. Servicio** `service/MuestraService.java`, en `copiarDatos`, debajo de `muestra.setFotoNombre(...)`
```java
muestra.setPesoG(request.getPesoG());
```

**6. Pruebas** `src/test/.../TestDatos.java` (los constructores de Lombok ganaron un parámetro): agregar `null` justo después de `"muestra_1.jpg"` en **las dos** llamadas.

### App

**7. Modelo** `data/model/Muestra.kt`: al final de `Muestra(...)`, después de `val remoteId: Long? = null` (agregarle una coma):
```kotlin
val pesoG: Double? = null
```

**8. Room** `data/local/entity/MuestraEntity.kt`: después de `val actualizadoEnEpochMs: Long` (con coma):
```kotlin
val pesoG: Double? = null
```
y en `data/local/AquaSampleDatabase.kt` cambiar `version = 2` por `version = 3`.

**9. Mappers** `data/local/MuestraMappers.kt`: en `aDominio` agregar `pesoG = muestra.pesoG` y en `aEntidad` agregar `pesoG = pesoG,`.

**10. Retrofit** `data/remote/MuestraRemota.kt`:
- en `MuestraRemotaRequest` y en `MuestraServidor`, al final: `val pesoG: Double? = null`
- en `aRemota()`: `pesoG = pesoG,`
- en `aMuestraLocal()`: `pesoG = pesoG`

**11. ViewModel** `ui/newSample/NewSampleViewModel.kt`:
- en `NewSampleUiState`, debajo de `val observaciones: String = "",` → `val pesoTexto: String = "",`
- en `aMuestra(...)`, debajo de `observaciones = observaciones,` → `pesoG = pesoTexto.aDecimal(),`
- función nueva:
  ```kotlin
  fun onPesoChange(texto: String) = _uiState.update { it.copy(pesoTexto = texto) }
  ```

**12. Pantalla** `ui/newSample/NewSampleGuardarScreen.kt`, justo antes de `Text("Observaciones", ...)`:
```kotlin
OutlinedTextField(
    value = uiState.pesoTexto,
    onValueChange = viewModel::onPesoChange,
    label = { Text("Peso de la muestra (g)") },
    singleLine = true,
    modifier = Modifier.fillMaxWidth()
)
Spacer(modifier = Modifier.height(16.dp))
```

**13. Detalle** `ui/sampledetail/SampleDetailViewModel.kt`: en el estado `val peso: String = "",` y en `conMuestra` `peso = muestra.pesoG?.let { "$it g" } ?: "—",`. En `SampleDetailScreen.kt`, debajo de la tarjeta "Operador":
```kotlin
Spacer(modifier = Modifier.height(12.dp))
DetailCard(modifier = Modifier.fillMaxWidth(), label = "Peso de la muestra", value = uiState.peso)
```

### Probarlo
1. Reiniciar el backend: Flyway aplica la V3 sola (se ve la columna `peso_g` en HeidiSQL).
2. Correr la app (Room v3 borra los datos locales una vez; es esperado en desarrollo).
3. Crear una muestra con peso → ver el peso en Detalle → en Swagger `GET /api/muestras` aparece `"pesoG"`.
4. En Swagger, `PUT` con otro `pesoG` → en la app, Historial → sincronizar → Detalle muestra el nuevo valor.

### Para ir rápido
- Repartir: una persona hace el backend (pasos 1 a 6) y otra la app (7 a 13) al mismo tiempo.
- Usar `Ctrl + Shift + F` (buscar en todo el proyecto) con el texto de referencia de cada paso, por ejemplo `fotoNombre` o `remoteId`.
- Si el profe pide "calibre promedio (mm)", son exactamente los mismos pasos cambiando el nombre.
