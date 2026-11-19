# M3 ISS · Etapa 2 · Clasificar la respuesta: `RemoteResult`

**Objetivo:** tratar cada resultado de red en la capa de datos, nunca en la pantalla.

**Qué se ve en la app:** al abrir la pantalla se consulta sola. Sin red aparece *"No hay conexión a
internet. Revisa el Wi-Fi o los datos móviles."* con un botón **Reintentar**
([captura](evidencia/e2-sin-conexion.png)), en vez del texto técnico de la etapa 1.

## Qué se construyó

| Archivo | Rol |
|---|---|
| `iss/data/remote/RemoteResult.kt` | `sealed interface RemoteResult<out T>`: `Success`, `HttpError(code)`, `RateLimited`, `Timeout`, `NoConnection`, `InvalidData`. Además, `map` y `safeApiCall { … }`. |
| `iss/data/IssRepository.kt` | `fetchPosition(): RemoteResult<IssPosition>`: ya no lanza excepciones |
| `iss/ui/IssUiState.kt` | `sealed interface IssUiState`: `Loading`, `Content(position)` y `Error(mensaje, puedeReintentar)`, más `RemoteResult.aError()` |
| `iss/ui/IssViewModel.kt` | convierte `RemoteResult` en `IssUiState` y aplica el control de frecuencia de 10 s |
| `iss/ui/IssScreen.kt` | un `when` exhaustivo sobre `IssUiState` |
| `res/values/strings.xml` | un mensaje por tipo de error; `tools:locale="es"` (ver la nota de lint) |
| `test/…/iss/data/remote/SafeApiCallTest.kt` | 8 pruebas con MockWebServer |
| `test/…/iss/ui/IssViewModelTest.kt` | 4 pruebas del ViewModel: control de frecuencia y reintentos |

```
OkHttp/Retrofit ─ excepción o Response ─► safeApiCall ─ RemoteResult ─► ViewModel ─ IssUiState ─► Pantalla
                  (SocketTimeoutException,                (valor, no excepción)   (@StringRes)
                   UnknownHostException, 404…)
```

## Conceptos

1. **Errores como valores.** Una excepción sale disparada y hay que acordarse de atraparla en
   cada lugar. Un `RemoteResult` es un valor que hay que mirar: la firma de `fetchPosition()`
   dice que puede fallar y cómo.
2. **`sealed` + `when` exhaustivo.** `aError()` y `map()` tratan las seis variantes sin `else`.
   Si se agrega una séptima, esas funciones dejan de compilar hasta que alguien decida qué hacer
   con ella.
3. **4xx, 429 y 5xx no son lo mismo.**

   | Código | Significa | ¿Reintentar? | Variante |
   |---|---|---|---|
   | 404 y otros 4xx | la solicitud está mal: la ruta, el id o los parámetros | no, fallará igual | `HttpError(404)`, `esErrorDelServidor = false` |
   | 429 | demasiadas solicitudes | sí, **después de esperar** | `RateLimited` |
   | 5xx | el servidor falló | sí, más tarde | `HttpError(500)`, `esErrorDelServidor = true` |
   | sin respuesta | sin red o DNS | sí, cuando haya red | `NoConnection` |
   | respuesta lenta | timeout | sí | `Timeout` |
   | 200 que no cumple el DTO | contrato roto | no | `InvalidData` |

4. **Por qué una excepción de red no debe llegar a la UI.** La pantalla tendría que conocer
   `SocketTimeoutException`, `UnknownHostException` y `JsonDataException`, todas de Java y de
   librerías de red, y el mensaje que vería el usuario sería el de la etapa 1. Con
   `RemoteResult`, `IssScreen` no importa nada de `retrofit2` ni de `okhttp3`.
5. **El orden de los `catch`.** `SocketTimeoutException`, `JsonEncodingException` y
   `EOFException` heredan de `IOException`, así que van antes que el caso general. Además,
   `CancellationException` se relanza: cancelar una coroutine al salir de la pantalla no es un
   error de red.
6. **Mensajes en `strings.xml`.** `IssUiState.Error` lleva un `@StringRes` y no un `String`, así
   que el ViewModel no necesita un `Context` y la app se puede traducir.

## Experimento guiado: el 429 y el control de frecuencia

1. Mostrar `respuesta429_esRateLimited` en `SafeApiCallTest`. Explicar que la API admite cerca de
   1 solicitud por segundo; las cabeceras reales de la etapa 1 decían
   `X-Rate-Limit-Limit: 350` en `5 minutes`. Si una app con miles de usuarios consulta en un
   bucle sin control, el servicio responde 429 a todos.
2. **Observación real (19-11-2026):** 12 solicitudes seguidas con `curl` devolvieron 12 veces
   `200`. El límite no se activa con pocas solicitudes, y por eso el 429 se prueba con
   MockWebServer en vez de esperar a que el servidor real lo devuelva.
3. En la app: abrir la pantalla (sale 1 solicitud) y tocar **Consultar** 8 veces seguidas. Logcat
   (`tag:okhttp.OkHttpClient`) registra **0 solicitudes nuevas**: los toques caen dentro de los
   10 s y el ViewModel los ignora. Pasados 10 s, el siguiente toque sí consulta.
4. El control está en `IssViewModel.consultar()`:
   - no hace nada si ya hay una consulta en curso;
   - no hace nada si la última consulta **exitosa** fue hace menos de `INTERVALO_MINIMO_MS`
     (10 s);
   - después de un error sí deja reintentar de inmediato: no hay datos nuevos que proteger.

   `RateLimited` es la red de seguridad: si de todas formas llegara un 429, el usuario lee
   "Se hicieron demasiadas consultas seguidas. Espera unos segundos."

## Evidencia (19-11-2026)

### Pruebas con MockWebServer (`./gradlew testDebugUnitTest`)

| Prueba | Respuesta del servidor falso | Resultado |
|---|---|---|
| `respuesta200ConJsonValido_esSuccessConElDominio` | 200 + JSON real | `Success`, además verifica la ruta `/v1/satellites/25544?units=kilometers` ✔ |
| `respuesta200ConJsonInvalido_esInvalidData` | 200 + `"latitude":"norte"` | `InvalidData` ✔ |
| `respuesta200QueNoEsJson_esInvalidData` | 200 + `<html>` | `InvalidData` ✔ |
| `respuesta404_esHttpErrorNoRecuperable` | 404 | `HttpError(404)` ✔ |
| `respuesta429_esRateLimited` | 429 | `RateLimited` ✔ |
| `respuesta500_esHttpErrorDelServidor` | 500 | `HttpError(500)` ✔ |
| `cuerpoQueTardaMasQueElTimeout_esTimeout` | `setBodyDelay(3 s)` con lectura de 1 s | `Timeout` ✔ |
| `servidorApagado_esNoConnection` | servidor apagado | `NoConnection` ✔ |

`IssViewModelTest` (4/4 ✔): 5 toques en 5 s → 1 solicitud; a los 10 s → 2 solicitudes; un 503
permite reintentar de inmediato; un 404 no ofrece reintentar.

### Verificación

| Comando | Resultado |
|---|---|
| `./gradlew assembleDebug assembleRelease` | OK |
| `./gradlew testDebugUnitTest` | OK; 12 pruebas nuevas, 0 fallas |
| `./gradlew lint` | 0 errores y 22 advertencias: las 19 previas y 3 `NewerVersionAvailable` de OkHttp 5.5.1 (okhttp, logging-interceptor y mockwebserver), justificadas en la etapa 1 |
| `grep -nE "^import (retrofit2\|okhttp3)" iss/ui/*.kt` | sin resultados |

**Nota de lint:** al agregar "en este momento", lint advirtió `Typos` ("¿quisiste decir
memento?") porque revisaba `strings.xml` como si estuviera en inglés. Se declaró
`tools:locale="es"` en `<resources>`, con un comentario.

## Criterios de aceptación

- [x] Las seis pruebas pedidas pasan, más dos adicionales: un 200 que no es JSON y el servidor
  apagado.
- [x] `IssScreen` no importa nada de `retrofit2` ni de `okhttp3`.
- [x] Control de frecuencia: los toques se ignoran durante 10 s después de una consulta exitosa.
