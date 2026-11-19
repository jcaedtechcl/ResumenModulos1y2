# M3 ISS · Etapa 4 · Detectar la conectividad y sincronizar al volver la red

**Objetivo:** reaccionar al estado de la red sin bloquear ni duplicar trabajo.

**Qué se ve en la app:**
- Mientras la pantalla está visible, la posición se actualiza sola cada ~10 s, con una barra fina
  de progreso arriba durante cada solicitud.
- Sin red aparece el banner *"Sin conexión: mostrando la última posición conocida"* encima de los
  datos ([captura](evidencia/e4-banner.png)).
- Al volver la red, el banner desaparece y llega una posición nueva sin tocar nada
  ([captura](evidencia/e4-red-volvio.png)).

## Qué se construyó

| Archivo | Rol |
|---|---|
| `iss/data/ConnectivityObserver.kt` | interfaz `ConnectivityObserver` + `AndroidConnectivityObserver`: `callbackFlow`, `registerDefaultNetworkCallback`, `awaitClose { unregisterNetworkCallback }`, emite el estado inicial y aplica `distinctUntilChanged()` |
| `AndroidManifest.xml` | `ACCESS_NETWORK_STATE` |
| `iss/data/IssRepository.kt` | `Mutex` en `refresh()` y `isRefreshing: StateFlow<Boolean>` |
| `iss/ui/IssUiState.kt` | `Empty`, `Content` y `Error` llevan `sinConexion` y `actualizando` |
| `iss/ui/IssViewModel.kt` | `combine(latest, history, ultimoError, enLinea, isRefreshing)` → un solo `IssUiState`; `actualizarMientrasVisible()` |
| `iss/ui/IssScreen.kt` | `LinearProgressIndicator`, banner y bucle con `repeatOnLifecycle(STARTED)` dentro de `LaunchedEffect` |
| `di/AppContainer.kt` | `connectivityObserver` |
| `test/…/IssViewModelTest.kt` | 4 pruebas nuevas: bucle, cancelación, reconexión y red intermitente |
| `test/…/IssRepositoryConcurrenciaTest.kt` | 3 refresh simultáneos → nunca más de 1 en curso |

```
ConnectivityManager ──callbackFlow──► isOnline ─┐
Room ── latest / history ──────────────────────┼─ combine ─► IssUiState ─► pantalla
IssRepository.isRefreshing ────────────────────┤
último error ──────────────────────────────────┘

pantalla STARTED ─► actualizarMientrasVisible(): isOnline.collectLatest { con red: refresh; delay(10 s); … }
pantalla STOPPED ─► cancelado
```

## Conceptos

1. **`callbackFlow`.** Convierte "registrar un callback → recibir avisos → desregistrar" en un
   `Flow`. Se registra al empezar a recolectar. `awaitClose` se ejecuta al cancelar la
   recolección y desregistra el callback; sin él, el callback quedaría registrado para siempre.
2. **Estado inicial y `distinctUntilChanged()`.** Si no hay red, el sistema no llama a ningún
   callback, así que se emite primero el estado actual. El sistema también avisa varias veces por
   la misma red, por ejemplo cuando cambia la señal, y `distinctUntilChanged` deja pasar solo los
   cambios.
3. **Conectividad como señal, no como garantía.** `INTERNET + VALIDATED` indica que *debería*
   haber internet, pero la API puede estar caída. Por eso cada refresh se sigue tratando con
   `RemoteResult`. La señal sirve para dos cosas: no intentar cuando seguro falla, y reaccionar
   cuando la red vuelve.
4. **`combine` en un único estado.** Cinco fuentes forman un solo `IssUiState`. La función que
   las une, `construirUiState`, es pura y se prueba sola. Sin red, el aviso de error se omite
   porque el banner ya lo explica.
5. **`Mutex`.** Es un candado para coroutines: la segunda **espera** su turno sin bloquear el
   hilo. El bucle, la reconexión, un toque y el worker de la etapa 5 comparten el mismo
   repositorio, y nunca hay dos solicitudes en curso. El ViewModel además **descarta** pedidos
   mientras `isRefreshing` es `true`, para que no queden en fila.
6. **Bucle atado al ciclo de vida.** `repeatOnLifecycle(STARTED)` ejecuta
   `actualizarMientrasVisible()` al quedar visible y la cancela en `onStop`. `collectLatest`
   sobre `isOnline` hace tres cosas:
   - con red, consulta y luego espera 10 s;
   - sin red, no intenta;
   - al pasar de `false` a `true`, reinicia el bloque, y eso **es** el refresh automático al
     reconectar.
7. **Respeto del límite de la API.** El bucle **espera la respuesta** y recién entonces cuenta
   10 s. El control de frecuencia de la etapa 2 absorbe reconexiones rápidas y toques extra.

### Ajuste durante la etapa (didáctico)

La primera versión del bucle llamaba a `consultar()`, que lanza la solicitud y **no espera** la
respuesta, y luego hacía `delay(10 s)`. En el emulador se midieron **2 solicitudes en 32 s**, no
las ~4 esperadas. La causa: la respuesta tardaba ~2 s y el control de frecuencia cuenta desde la
respuesta exitosa, así que cada vuelta caía a los 8 s de la anterior y se ignoraba. Se separó
`actualizarSiCorresponde()` (`suspend`, termina con la respuesta) de `consultar()` (el toque, que
la lanza sin esperar), y el bucle usa la primera. La lección: **un `delay` mide desde donde está,
no desde donde uno cree**.

## Experimento guiado (emulador, 19-11-2026)

Con Logcat filtrado por `tag:okhttp.OkHttpClient`, o con el **Network Inspector**:

| Escenario | Observado |
|---|---|
| Pantalla visible 40 s | solicitudes a las 19:21:35.7, 19:21:46.9, 19:21:58.3 y 19:22:09.5, cada ~11 s (10 s + la respuesta) |
| Botón Inicio, 40 s en segundo plano | **0 solicitudes** |
| Modo avión on → off, 3 veces en ~18 s | **1 sola solicitud** (`--> GET` 19:23:28.1 / `<-- 200` 19:23:28.9). Las otras reconexiones cayeron dentro de los 10 s y se descartaron. Ninguna se solapó. |
| Modo avión on | aparece el banner sobre los datos ([captura](evidencia/e4-banner.png)) |
| Modo avión off | el banner desaparece y llega una posición nueva sola ([captura](evidencia/e4-red-volvio.png)) |

Pregunta para la clase: *¿por qué no se ve una solicitud fallida cada 10 s mientras está el
modo avión?* Porque `collectLatest` recibió `false` y el bloque termina sin entrar al bucle. No
se intenta lo que seguro falla.

## Evidencia de pruebas (`./gradlew testDebugUnitTest`)

| Prueba | Qué demuestra |
|---|---|
| `bucleVisible_consultaAlEmpezarYCada10s` | t = 0, 10, 20 y 30 s → 4 solicitudes, con tiempo virtual |
| `bucleCancelado_noHaceMasSolicitudes` | cancelar el bucle, como hace `repeatOnLifecycle` en `onStop`, → 0 solicitudes más en 60 s |
| `sinRed_noIntenta_yAlVolverLaRed_consultaDeInmediato` | 30 s sin red → 0 solicitudes y estado `Empty(sinConexion = true)`; `false → true` → 1 solicitud |
| `redQueVaYViene_rapido_noDuplicaSolicitudes` | 3 reconexiones en 3 s → 1 solicitud |
| `IssRepositoryConcurrenciaTest.tresRefreshSimultaneos_seEjecutanDeUnoEnUno` | 3 `refresh()` lanzados a la vez → 3 solicitudes, **máximo 1 simultánea**, e `isRefreshing` vuelve a `false` |
| `IssUiStateTest.sinConexion_elBannerReemplazaAlAviso_yLosDatosSiguen` | sin red, los datos se ven, el aviso se oculta y `sinConexion = true` |

### Verificación

| Comando | Resultado |
|---|---|
| `./gradlew assembleDebug assembleRelease` | OK |
| `./gradlew testDebugUnitTest` | OK; 23 pruebas de ISS |
| `./gradlew assembleDebugAndroidTest` | OK |
| `./gradlew lint` | 0 errores y 22 advertencias, sin cambios |
| imports de `retrofit2`, `okhttp3`, `androidx.room` o `androidx.work` en `IssScreen.kt` | ninguno |

## Criterios de aceptación

- [x] El banner aparece y desaparece solo (capturas y `sinRed_noIntenta_…`).
- [x] Nunca hay más de una solicitud en curso (`Mutex`, `IssRepositoryConcurrenciaTest` y el
  experimento del modo avión).
- [x] No hay solicitudes con la app en segundo plano por el bucle de primer plano (0 en 40 s y
  `bucleCancelado_noHaceMasSolicitudes`).
