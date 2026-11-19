# M3 ISS · Etapa 1 · Primera llamada con Retrofit

**Objetivo:** consumir una API REST y mostrar su respuesta en Compose.

**Qué se ve en la app:** menú ⋮ (*Ejemplos*) → **¿Dónde está la ISS?** → **Consultar**. Aparecen la
latitud, la longitud, la altitud, la velocidad, la visibilidad y la hora de la medición
([captura](evidencia/e1-consulta.png)).

## Verificación previa de la API

```
$ curl -s "https://api.wheretheiss.at/v1/satellites/25544?units=kilometers"
{"name":"iss","id":25544,"latitude":-20.666074804477,"longitude":-154.2053938432,
 "altitude":422.19160851825,"velocity":27572.641214846,"visibility":"daylight",
 "footprint":4518.5966167313,"timestamp":1795124992,"daynum":2461364.4096296,
 "solar_lat":-19.617277823589,"solar_lon":208.89853975337,"units":"kilometers"}
```

El contrato coincide con la consigna, así que el DTO no requirió cambios.

## Decisiones respecto de la consigna

| Consigna | Proyecto | Motivo |
|---|---|---|
| Paquete `cl.ipp.recordatorios` | `com.example.resumen_modulos1y2` | Es el paquete real de la app. La estructura pedida se respeta bajo `…/iss/` (`data/remote`, `data/local`, `data`, `domain`, `work`, `ui`). |
| Retrofit 3.0.0, converter-moshi 3.0.0 | igual | Son las últimas estables (Maven Central, 19-11-2026). |
| OkHttp 4.12.0 y logging-interceptor | igual | Ya existe OkHttp 5.5.1, pero Retrofit 3.0.0 se compila contra 4.12.0 y la lectura usa esa versión. Lint lo informa como advertencia `NewerVersionAvailable` (justificada). |
| Moshi 1.15.2 y moshi-kotlin-codegen (KSP) | igual | Es la última estable y funciona con KSP 2.3.14, que el proyecto ya usaba para Room. |
| `HttpLoggingInterceptor` "solo si `BuildConfig.DEBUG`" | `debugImplementation` + `src/debug` / `src/release` | Con el `if` la clase igual viaja en el APK de release, porque este proyecto no usa R8. Con conjuntos de fuentes por tipo de build, el APK de release **no la contiene** (ver la evidencia). |
| Acceso "desde la navegación existente" | Ítem del menú *Ejemplos* (enum `Leccion`) | Es el menú que ya abre las demás pantallas. El título del ítem sigue el patrón existente de `Leccion` (texto en el enum); el resto de los textos de la pantalla están en `strings.xml`. |

## Qué se construyó

| Archivo | Rol |
|---|---|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | Retrofit, converter-moshi, OkHttp, Moshi y codegen por KSP. `buildConfig = true`. |
| `gradle.properties` | `iss.baseUrl=https://api.wheretheiss.at/` |
| `app/build.gradle.kts` | `buildConfigField("String", "ISS_BASE_URL", …)` lee esa propiedad |
| `AndroidManifest.xml` | `INTERNET` (ya existía) |
| `iss/data/remote/IssPositionDto.kt` | el contrato JSON, con `@JsonClass(generateAdapter = true)` y `@Json(name = …)` |
| `iss/data/remote/IssApiService.kt` | `@GET("v1/satellites/{id}")` con `@Path`, `@Query` y `Response<IssPositionDto>` |
| `src/debug/…/RegistroDeRed.kt`, `src/release/…/RegistroDeRed.kt` | `agregarRegistroDeRed()`: nivel BODY en debug, nada en release |
| `di/AppContainer.kt` | un `OkHttpClient` (10 s de conexión y 15 s de lectura), un `Retrofit` y el `IssApiService` |
| `iss/domain/IssPosition.kt`, `iss/data/Mappers.kt` | el modelo que ve la UI y la traducción DTO → dominio |
| `iss/data/IssRepository.kt` | `fetchPosition()`: llama a la API y lanza una excepción si algo falla |
| `iss/ui/IssViewModel.kt`, `iss/ui/IssScreen.kt` | estado mínimo (`ConsultaIss`), el botón "Consultar" y un enlace `geo:` para abrir el mapa |

```
IssScreen ──"Consultar"──► IssViewModel ──► IssRepository ──► IssApiService (Retrofit)
    ▲                          │                  │ toDomain()        │ OkHttp ─► HTTPS
    └──── StateFlow<ConsultaIss> ◄────────────────┴───────────────────┘
```

## Conceptos

1. **El DTO es un contrato.** Copia el JSON tal cual. La app no trabaja con el DTO: lo traduce a
   `IssPosition`, que tiene unidades en los nombres, `Instant` en vez de segundos sueltos y un
   `enum` en vez de `"daylight"`. Si la API cambia, se toca el DTO y el mapeador, nada más.
2. **`@GET`, `@Path` y `@Query`.** La interfaz *describe* la solicitud
   `GET v1/satellites/25544?units=kilometers` y Retrofit genera el código que la ejecuta.
3. **`Response<T>`.** Entrega el código HTTP. Con `suspend fun …(): IssPositionDto`, cualquier
   código que no sea 2xx sería una `HttpException` difícil de clasificar. En la etapa 2 esto se
   aprovecha para distinguir 404, 429 y 500.
4. **Una sola instancia del cliente.** OkHttp mantiene un pool de conexiones y de hilos. Si se
   crea uno por llamada, cada consulta repite el saludo TLS y deja hilos huérfanos. Por eso viven
   en `AppContainer` con `by lazy`.
5. **La URL fuera del código.** Se puede cambiar por entorno sin tocar Kotlin, por ejemplo con
   `./gradlew installDebug -Piss.baseUrl=…`, como en el experimento 2. En las pruebas se usa la
   URL de un MockWebServer.
6. **Timeouts.** Sin ellos, una red que acepta la conexión pero nunca responde deja el indicador
   de carga girando para siempre.
7. **Consumo responsable.** La API permite cerca de 1 solicitud por segundo. Las cabeceras reales
   lo confirman: `X-Rate-Limit-Limit: 350` y `X-Rate-Limit-Interval: 5 minutes`. La app no
   consultará más de una vez cada 10 s; el control se implementa en la etapa 2.

## Experimento guiado (≈ 30 min)

### 1. Sin red

Activar el modo avión y tocar **Consultar**:

```
Error: Unable to resolve host "api.wheretheiss.at": No address associated with hostname
```

([captura](evidencia/e1-modo-avion.png)). La pantalla muestra el texto técnico de una
`UnknownHostException`, y la posición anterior se perdería al cerrar la app.
- *¿Le sirve este mensaje a un usuario?* No: motiva la etapa 2.
- *¿Por qué no hay nada que mostrar sin red?* Porque no se guarda nada: motiva la etapa 3.

### 2. Tráfico sin cifrar (Open Notify por HTTP)

Sin editar ningún archivo, se instala con otra URL base:

```
./gradlew installDebug -Piss.baseUrl=http://api.open-notify.org/
```

Al tocar **Consultar** ([captura](evidencia/e1-cleartext.png)):

```
Error: CLEARTEXT communication to api.open-notify.org not permitted by network security policy
```

La solicitud nunca sale del teléfono. Desde Android 9 (API 28), la política de seguridad de red
bloquea HTTP sin cifrar por defecto: cualquiera en la misma red Wi-Fi podría leer o modificar la
respuesta. La salida fácil, `android:usesCleartextTraffic="true"`, desactiva esa protección
**para toda la app y todos los dominios**. Si de verdad hiciera falta un dominio HTTP, lo correcto
es un `network_security_config.xml` que lo permita solo para ese dominio. Lo mejor es usar HTTPS,
como hace la API principal.

Para volver a la normalidad: `./gradlew installDebug` (sin `-P`).

### 3. Ver la solicitud

- **Logcat**, filtro `tag:okhttp.OkHttpClient`:

  ```
  --> GET https://api.wheretheiss.at/v1/satellites/25544?units=kilometers
  <-- 200 OK https://api.wheretheiss.at/v1/satellites/25544?units=kilometers (2041ms)
  X-Rate-Limit-Limit: 350
  X-Rate-Limit-Remaining: 349
  X-Rate-Limit-Interval: 5 minutes
  {"name":"iss","id":25544,"latitude":-2.83, …}
  ```

- **App Inspection → Network Inspector**: con la app en debug, cada **Consultar** aparece en la
  línea de tiempo. Al seleccionarla se ven la URL, las cabeceras y el JSON de la respuesta.

## Evidencia (emulador, 19-11-2026)

- La pantalla muestra datos reales: latitud −2.8328°, longitud −140.6708°, 416.0 km y
  27,592 km/h, iluminada por el Sol ([captura](evidencia/e1-consulta.png)).
- El interceptor no existe en release. Se buscó la clase en los `.dex` de cada APK:

  ```
  debug:   HttpLoggingInterceptor refs=6
  release: HttpLoggingInterceptor refs=0
  ```

- No hay URL en el código Kotlin: `grep -rn "https\?://" app/src/main/java --include=*.kt` no
  encuentra ninguna fuera de comentarios.

### Verificación

| Comando | Resultado |
|---|---|
| `./gradlew assembleDebug assembleRelease` | OK |
| `./gradlew lint` | 0 errores y 21 advertencias. Las 19 previas siguen igual y se suman 2 `NewerVersionAvailable` de OkHttp 5.5.1, justificadas arriba. |
| `./gradlew testDebugUnitTest` | OK; esta etapa no agrega pruebas |

## Criterios de aceptación

- [x] La pantalla muestra datos reales.
- [x] No hay URL escrita en clases Kotlin (está en `gradle.properties` → `BuildConfig`).
- [x] El interceptor de registro no existe en el build de release.
