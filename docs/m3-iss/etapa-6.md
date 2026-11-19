# M3 ISS · Etapa 6 · Evidencia, conexión con la app y cierre

**Objetivo:** documentar con evidencia y conectar la funcionalidad con el dominio de la app.

**Qué se ve en la app:**
1. Debajo de la tarjeta de posición está el botón **Crear recordatorio de observación**. Al
   tocarlo aparece *"Recordatorio creado. Lo encontrarás en Recordatorios."*
2. En ⋮ → *Recordatorios · DataStore* aparece **Observar la ISS**, con la última posición en la
   descripción ([captura](evidencia/e6-recordatorio-creado.png)).

## Qué se construyó

| Archivo | Rol |
|---|---|
| `iss/ui/IssViewModel.kt` | `crearRecordatorioDeObservacion(titulo, descripcion, categoria)`: arma un `Recordatorio` con un UUID y lo guarda con `guardarRecordatorio` |
| `iss/ui/IssViewModel.kt` (Factory) | `guardarRecordatorio = container.reminderRepository::guardar`: **reutiliza el repositorio existente** |
| `iss/ui/IssScreen.kt` | botón, textos desde `strings.xml` con la posición formateada y `Snackbar` de confirmación |
| `res/values/strings.xml` | título "Observar la ISS", categoría "Observación", plantilla de la descripción y confirmación |
| `test/…/IssViewModelTest.kt` | `crearRecordatorioDeObservacion_loGuardaConElRepositorioDeRecordatorios` |
| `docs/m3-iss/evidencias.md` | tabla de escenarios lista para completar con capturas, ya llena con lo observado |
| `docs/m3-iss/README.md` | índice, estructura y versiones |

## Conceptos

1. **Reutilizar, no duplicar.** La pantalla de la ISS no escribe en la tabla `reminders` por su
   cuenta: usa `ReminderRepository.guardar`, el mismo que usa el formulario de recordatorios. Si
   mañana cambian las reglas para guardar un recordatorio, por ejemplo `updated_at`, hay un solo
   lugar que tocar.
2. **Room avisa a todos.** La lista de recordatorios observa `ReminderDao.observeAll()`. Al
   guardar desde la pantalla de la ISS, la lista ya tiene el recordatorio nuevo la próxima vez
   que se abre, sin recargar nada. Es el mismo principio de una sola fuente de verdad de la
   etapa 3.
3. **Dependencia mínima.** El ViewModel no recibe todo el `ReminderRepository`: recibe la función
   `suspend (Recordatorio) -> Unit`. En la prueba basta una lambda que agrega a una lista.
4. **Los textos no viven en el ViewModel.** El título y la descripción se resuelven con
   `stringResource` en la pantalla y llegan al ViewModel ya escritos. El ViewModel sigue sin
   necesitar un `Context`.
5. **Latitud y longitud.** La consigna pedía guardarlas "si la entidad de recordatorios tiene
   esos campos". `ReminderEntity` no los tiene, y agregarlos exigiría una migración de
   `reminders` y modificar la funcionalidad existente, que la consigna pide no tocar. Por eso la
   posición va en la descripción.

## Evidencia (emulador, 19-11-2026)

- Recordatorio creado desde la ISS y visto en la lista:

  ```
  Observar la ISS
  Última posición conocida (19:51:38): latitud -25.4845°, longitud 49.4133°, altitud 430.4 km.
  En la sombra de la Tierra.
  Sin fecha · Observación                                     Media
  ```

- Pantalla completa con la posición, el botón nuevo, la tarjeta de segundo plano y el historial
  ([captura](evidencia/e6-pantalla-completa.png)).
- Tabla de escenarios: [evidencias.md](evidencias.md).

### Verificación final

| Comando | Resultado |
|---|---|
| `./gradlew assembleDebug` | OK |
| `./gradlew assembleRelease` | OK. `HttpLoggingInterceptor` en el dex: debug 6 referencias, **release 0** |
| `./gradlew lint` | **0 errores** y 22 advertencias, justificadas abajo |
| `./gradlew testDebugUnitTest` | **44 pruebas, 0 fallas** (24 de ISS) |
| `./gradlew connectedDebugAndroidTest` | **35 pruebas, 0 fallas** en el emulador `emulator-5554` (14 de ISS) |

Reglas de la consigna, verificadas con `grep` sobre `app/src`:

| Regla | Resultado |
|---|---|
| Sin `runBlocking`, `GlobalScope` ni `allowMainThreadQueries()` | ninguna aparición |
| Ningún composable importa Retrofit, OkHttp, Room ni WorkManager | `iss/ui/` no tiene imports de `retrofit2`, `okhttp3`, `androidx.room` ni `androidx.work` |
| Los DTO nunca llegan a la UI | `IssPositionDto` solo se usa en `data/` (y en pruebas) |
| Strings visibles en `strings.xml` | todos los textos de la pantalla ISS; el título del menú sigue el patrón previo de `Leccion` (etapa 1) |

### Advertencias de lint (22), justificadas

| Advertencia | Cantidad | Justificación |
|---|---|---|
| `NewerVersionAvailable` OkHttp 5.5.1 (okhttp, logging-interceptor, mockwebserver) | 3 | Se usa la 4.12.0, alineada con Retrofit 3.0.0 y la lectura (etapa 1) |
| Previas al módulo ISS (versiones de AGP, Kotlin, Compose BOM, lifecycle y coroutines-test; colores sin uso; `OldTargetApi`; `RedundantLabel`; `ModifierParameter` en `EstadoScreen`) | 19 | Ya existían antes de esta actividad; corregirlas tocaría funcionalidades que la consigna pide no modificar |

## Criterios de aceptación

- [x] Botón "Crear recordatorio de observación" que crea "Observar la ISS" con la última
  posición, usando el repositorio de recordatorios existente.
- [x] `evidencias.md` con los 8 escenarios pedidos más el del recordatorio, con lo observado y
  las capturas.
