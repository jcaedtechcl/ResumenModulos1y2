# M3 ISS · Etapa 5 · WorkManager: actualizar aunque la app esté cerrada

**Objetivo:** delegar al sistema un trabajo confiable, con restricciones y reintentos.

**Qué se ve en la app:** la tarjeta **Actualización en segundo plano** muestra el estado de los dos
trabajos, por ejemplo *Cada 15 min: En cola · 19:47* e *Inmediata: Completada · 19:35*. También
tiene el botón **Actualizar en segundo plano**.

## Qué se construyó

| Archivo | Rol |
|---|---|
| `iss/work/IssSyncWorker.kt` | `CoroutineWorker` que llama a `refresh()` y traduce el resultado a `Result`; incluye `IssWorkerFactory` |
| `iss/work/IssWorkScheduler.kt` | interfaz `BackgroundSync` + `IssWorkScheduler`: periódico de 15 min, trabajo único inmediato y observación del estado |
| `iss/domain/EstadoSincronizacion.kt` | `FaseTrabajo` y `EstadoTrabajo`: el estado sin tipos de WorkManager |
| `iss/data/IssRepository.kt` | `fun interface IssRefresher`, que el repositorio implementa |
| `RecordatoriosApp.kt` | `Configuration.Provider` con `IssWorkerFactory`; programa el periódico en `onCreate` |
| `AndroidManifest.xml` | desactiva la inicialización automática de WorkManager (`tools:node="remove"`) |
| `di/AppContainer.kt` | `issWorkScheduler` |
| `iss/ui/IssViewModel.kt`, `IssScreen.kt` | `estadoSegundoPlano` y `actualizarEnSegundoPlano()`; la tarjeta del estado |
| `androidTest/…/iss/work/IssSyncWorkerTest.kt` | 4 pruebas con `TestListenableWorkerBuilder` y un repositorio falso |
| `libs.versions.toml` | `work-runtime-ktx` y `work-testing` **2.12.0**, la estable vigente en Google Maven el 19-11-2026 |

### Mapeo de resultados

| `RemoteResult` | `Result` | Por qué |
|---|---|---|
| `Success` | `success(terminado_en)` | la hora viaja en `outputData` y la pantalla la muestra |
| `Timeout`, `NoConnection`, `RateLimited`, 5xx | `retry()` | transitorios: más tarde puede funcionar |
| 4xx, `InvalidData` | `failure()` | permanentes: repetir da el mismo error |
| cualquier fallo con `runAttemptCount ≥ 5` | `failure()` | no reintentar para siempre |

## Conceptos

1. **¿WorkManager o una coroutine?**

   | | coroutine (`viewModelScope`, bucle de la etapa 4) | WorkManager |
   |---|---|---|
   | vive | mientras la pantalla o el proceso | en la base de WorkManager, aun con la app cerrada o tras reiniciar |
   | cuándo corre | ahora | cuando el sistema decide y se cumplen las restricciones |
   | para qué | trabajo **inmediato** mientras el usuario mira | trabajo que **debe** ocurrir tarde o temprano |

2. **`CoroutineWorker`.** `doWork()` es `suspend` y corre fuera del hilo principal. Si el sistema
   detiene el trabajo, por ejemplo porque se perdió la red, la coroutine se cancela.
3. **Restricciones.** `Constraints(requiredNetworkType = CONNECTED)`: sin red, el trabajo **no
   falla, espera**.
4. **Backoff exponencial.** Tras un `retry()`, el periódico espera 30 s, luego 60, 120 y así,
   con un máximo de 5 h. Un servidor caído no recibe una avalancha de reintentos.
5. **Trabajo único con `KEEP`.**
   - `enqueueUniquePeriodicWork("iss_periodic_sync", KEEP, …)` se llama en **cada** arranque.
     Si ya existe, se conserva el existente, así que nunca hay dos.
   - `enqueueUniqueWork("iss_sync_now", KEEP, …)`: mientras uno está en cola o en ejecución,
     los toques extra no crean otro.
6. **15 minutos es el mínimo.** Con menos, WorkManager usa 15 de todos modos.
7. **Los tiempos no son exactos (Doze).** Con la pantalla apagada y el teléfono quieto, Android
   agrupa los trabajos en ventanas de mantenimiento para ahorrar batería. "Cada 15 min" significa
   "no antes de 15 min": puede ser bastante después. Para algo a una hora exacta se usa
   `AlarmManager`, y aun así con limitaciones.
8. **Dependencias en el worker.** WorkManager crea los workers por reflexión. `IssWorkerFactory`,
   registrada con `Configuration.Provider`, les entrega el `IssRepository` de `AppContainer`. Es
   la misma instancia que usa la pantalla y comparte el `Mutex` de la etapa 4, así que el worker
   y el bucle nunca consultan a la vez.
9. **Interfaz mínima.** El worker solo necesita `refresh()`, así que depende de
   `fun interface IssRefresher`. El "repositorio falso" de la prueba es una línea:
   `IssRefresher { RemoteResult.Timeout }`.

## Experimento guiado (emulador, 19-11-2026)

### 1. El trabajo periódico existe una sola vez

Se abrió la app 5 veces seguidas, con `force-stop` entre cada apertura, y se pidió el
diagnóstico de WorkManager:

```
adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.example.resumen_modulos1y2
# Logcat, tag WM-DiagnosticsWrkr:
Enqueued work:
 9799fdf8-…  com.example.resumen_modulos1y2.iss.work.IssSyncWorker  0  ENQUEUED  iss_periodic_sync
```

Aparece **una sola** fila `iss_periodic_sync` y un solo job en `dumpsys jobscheduler`.
En Android Studio se ve lo mismo en **App Inspection → Background Task Inspector**.

### 2. Forzar el periódico y la ventana de 15 min

`adb shell cmd jobscheduler run -f -n androidx.work.systemjobscheduler <paquete> <jobId>` despierta
el job, pero WorkManager respondió:

```
WM-WorkerWrapper: Delaying execution for …IssSyncWorker because it is being executed before schedule.
```

El periódico ya había corrido al instalar la app, y WorkManager **no adelanta** la siguiente
ejecución aunque se fuerce el job del sistema, porque respeta su propio período. Lección: los
tiempos los decide el sistema. Para forzarlo desde el **Background Task Inspector** pasa lo mismo
dentro de la ventana; lo práctico en clase es usar el botón del trabajo inmediato o esperar la
ventana, como en el punto 4.

### 3. "Actualizar en segundo plano" ×5 con el modo avión

Con el modo avión activo, se tocó el botón **5 veces** en 2 s
([captura](evidencia/e5-en-cola-sin-red.png)):

```
Enqueued work:
 9799fdf8-…  IssSyncWorker  3  ENQUEUED  iss_periodic_sync
 a3518d4d-…  IssSyncWorker  4  ENQUEUED  iss_sync_now        ← uno solo, gracias a KEEP
```

La tarjeta mostraba *Inmediata: En cola*. El trabajo **no falló**: la restricción `CONNECTED` lo
mantuvo esperando.

Al desactivar el modo avión (19:35:03), el sistema lo ejecutó solo y salió `--> GET …` a las
19:35:04. La tarjeta pasó a *Inmediata: Completada · 19:35*
([captura](evidencia/e5-completado-al-volver-red.png)).

### 4. Una posición que la app no pidió

1. Con la pantalla abierta, el último registro del historial era el de las **19:35:28**.
2. Se cerró la app por completo: botón Inicio y luego `adb shell am kill …`, con lo que no
   quedó ningún proceso (`pidof` vacío, 19:35:36). **No** se usó `force-stop`, porque cancela los
   trabajos programados.
3. Al cumplirse la ventana de 15 min, el **sistema** creó el proceso y ejecutó el worker


   ```
   19:50:22.398 Zygote  : Process 13153 created for com.example.resumen_modulos1y2
   19:50:23.200 WM-WorkerWrapper: Starting work for …iss.work.IssSyncWorker
   19:50:23.236 okhttp.OkHttpClient: --> GET https://api.wheretheiss.at/v1/satellites/25544?units=kilometers
   19:50:24.893 okhttp.OkHttpClient: <-- 200 OK … (1656ms)
   19:50:24.923 WM-WorkerWrapper: Worker result SUCCESS for Work [ id=9799fdf8-… ]
   19:50:24.945 WM-SystemJobScheduler: Scheduling work ID 9799fdf8-… Job ID 3   ← próximo período
   ```

4. Al reabrir la app, el historial tenía una fila de las **19:50:25** que la app no pidió:
   `19:50:56 · 19:50:45 · 19:50:25 · 19:35:28 · …`. Las dos primeras son del bucle de la
   pantalla, al volver a abrirla. La tarjeta mostraba *Cada 15 min: En cola · 20:05*
   ([captura](evidencia/e5-posicion-con-app-cerrada.png)).

El trabajo corrió a las 19:50, no a las 19:47 que mostraba la tarjeta: el sistema decide el
momento exacto (punto 7 de Conceptos).

## Evidencia de pruebas

`IssSyncWorkerTest` (`./gradlew connectedDebugAndroidTest`, 4/4 ✔; en total **35 tests, 0
failures**):

| Prueba | Verifica |
|---|---|
| `success_esSuccess_conLaHoraDeTermino` | `Success` → `Result.success` con `terminado_en` |
| `fallosTransitorios_sonRetry` | `Timeout`, `NoConnection`, `RateLimited` y 503 → `Result.retry()` |
| `fallosPermanentes_sonFailure` | 404 e `InvalidData` → `Result.failure()` |
| `trasElMaximoDeIntentos_unFalloTransitorioEsFailure` | intento 4 → `retry`; intento 5 → `failure` |

### Verificación

| Comando | Resultado |
|---|---|
| `./gradlew assembleDebug assembleRelease` | OK |
| `./gradlew testDebugUnitTest` | OK |
| `./gradlew connectedDebugAndroidTest` | 35/35 OK |
| `./gradlew lint` | 0 errores y 22 advertencias, sin cambios |
| imports de `androidx.work` en `iss/ui/` | ninguno: la pantalla recibe `EstadoSincronizacion` |

## Criterios de aceptación

- [x] El trabajo periódico existe una sola vez aunque la app se abra muchas veces
  (experimento 1).
- [x] La prueba del worker pasa (`IssSyncWorkerTest`, 4/4).
