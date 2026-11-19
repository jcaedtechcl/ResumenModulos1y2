# M3 ISS · Etapa 3 · Room como fuente de verdad: uso sin conexión

**Objetivo:** que la pantalla funcione sin red mostrando la última información conocida.

**Qué se ve en la app:** una tarjeta con la última posición y su antigüedad ("Medida hace 1 min"),
el botón **Consultar** y el **historial reciente**. Sin red, todo sigue visible, con el aviso de
error arriba ([captura](evidencia/e3-sin-conexion-tras-reabrir.png)).

## Qué se construyó

| Archivo | Rol |
|---|---|
| `iss/data/local/IssPositionEntity.kt` | tabla `iss_positions`, clave primaria `timestamp` |
| `iss/data/local/IssPositionDao.kt` | `observeLatest()`, `observeHistory(limit = 20)`, `@Upsert save` y `deleteOlderThan` |
| `data/local/AppDatabase.kt` | `version = 3`, con la entidad nueva en la **misma** base de los recordatorios |
| `data/local/Migrations.kt` | `MIGRATION_2_3`: `CREATE TABLE iss_positions …`. No se usa una migración destructiva, por la misma razón de la actividad de Room. |
| `app/schemas/…/3.json` | esquema exportado de la versión 3 |
| `iss/data/Mappers.kt` | DTO → Entity → dominio |
| `iss/data/IssRepository.kt` | `latest` y `history` vienen de Room; `refresh(): RemoteResult<Unit>` consulta y guarda; conserva 24 h |
| `iss/ui/IssUiState.kt` | `Loading`, `Empty`, `Content(position, history, error?)` y `Error`; `construirUiState` y `antiguedad` son funciones puras |
| `iss/ui/IssViewModel.kt` | `combine(latest, history, ultimoError)` → `stateIn` |
| `iss/ui/IssScreen.kt` | tarjeta con antigüedad, historial con clave estable, vacío distinto de error |
| `src/debug/res/xml/network_security_config.xml` | HTTP solo hacia `localhost`, solo en debug, para MockWebServer en el emulador |

```
                 refresh()                         Flow (cada cambio de la tabla)
  API ─────────► IssRepository ──save()──► Room ───────────────────────────────► ViewModel ─► pantalla
 (puede fallar)        │                  (iss_positions)                           ▲
                       └── RemoteResult<Unit> (¿funcionó?) ─────────────────────────┘
```

## Conceptos

1. **Local-first.** La pantalla nunca muestra lo que llegó por la red: muestra lo que hay en
   Room. La red solo **actualiza** Room, y Room actualiza la pantalla. Por eso `refresh()`
   devuelve `RemoteResult<Unit>`: avisa si funcionó, pero no entrega la posición.
2. **Una sola fuente de verdad.** Si el ViewModel guardara la posición recibida y además leyera
   Room, habría dos versiones que pueden no coincidir. Con una sola fuente, lo que se ve es
   siempre lo que está guardado.
3. **Vacío ≠ error.**

   | Room | Última consulta | Estado | Se ve |
   |---|---|---|---|
   | sin filas | sin error | `Empty` | "Aún no hay posiciones guardadas…" |
   | sin filas | falló | `Error` | mensaje y Reintentar |
   | con filas | bien | `Content(error = null)` | datos |
   | con filas | falló | `Content(error = …)` | **datos y aviso**: el error no oculta nada |

   `construirUiState` es una función pura y se prueba con un test unitario simple.
4. **Clave natural y `@Upsert`.** La medición se identifica por su `timestamp`. Si el mismo dato
   llega dos veces, se reemplaza en vez de duplicarse.
5. **Retención.** Después de cada guardado se borra lo anterior a 24 h, para que el historial no
   crezca sin límite.
6. **Antigüedad como función pura.** `antiguedad(medida, ahora)` no lee el reloj: lo recibe. La
   pantalla le pasa un "ahora" que se actualiza cada 30 s, así que "hace 3 min" avanza sin pedir
   nada a la red. El texto sale de `plurals` en `strings.xml`.
7. **Una base, varias funcionalidades.** La tabla se agregó a la `AppDatabase` existente con una
   migración 2→3, sin tocar `reminders`. `MigrationTest` lo verifica.

## Experimento guiado (emulador, 19-11-2026)

1. Con red, abrir **¿Dónde está la ISS?** (se guardó la medición de las 19:12:23) y tocar
   **Consultar** pasados 10 s (se guardó la de las 19:12:40).
2. Activar el modo avión (`adb shell cmd connectivity airplane-mode enable`).
3. Forzar la detención (`adb shell am force-stop com.example.resumen_modulos1y2`) y esperar un
   minuto.
4. Reabrir la app y la pantalla. Se observó
   ([captura](evidencia/e3-sin-conexion-tras-reabrir.png)):

   ```
   No hay conexión a internet. Revisa el Wi-Fi o los datos móviles.
   Medida hace 1 min
   Latitud 43.9403° · Longitud -93.3761° · 417.7 km · 27,608 km/h · Iluminada por el Sol
   Historial reciente
     19:12:40  Lat. 43.9403° · Lon. -93.3761°
     19:12:23  Lat. 43.3703° · Lon. -94.6030°
   ```

   El proceso era nuevo y no había red, pero la pantalla no quedó en blanco: Room tenía los
   datos. El intento de actualizar falló, y ese error se muestra **encima** de los datos.

Pregunta para la clase: *en la etapa 1, este mismo experimento mostraba solo un error. ¿Qué
cambió?* El origen de lo que se dibuja: antes era la respuesta de red, ahora es la base.

## Evidencia de pruebas

**Unitarias** (`./gradlew testDebugUnitTest`):
- `IssUiStateTest` (4): antigüedad redondeada, reloj atrasado, vacío ≠ error y contenido con error.
- `IssViewModelTest` (5): usa una API falsa y `FakeIssPositionDao`.
- `SafeApiCallTest` (8): ahora prueba `safeApiCall` directamente.

**Instrumentadas** (`./gradlew connectedDebugAndroidTest`): **31 tests, 0 failures**. Las nuevas:

| Prueba | Qué demuestra |
|---|---|
| `IssPositionDaoTest` (5) | tabla vacía → `null`; `latest` es la medición más reciente aunque se guarde tarde; historial descendente con límite 20; mismo timestamp → 1 fila; `deleteOlderThan` |
| `IssRepositoryTest.refreshExitoso_guardaUnaFila_yLatestLaEmite` | MockWebServer → Room: una fila |
| `IssRepositoryTest.refreshFallido_noBorraLoAnterior` | un 500 después de un éxito deja la fila intacta |
| `IssRepositoryTest.refreshRepetidoConElMismoTimestamp_noDuplicaFilas` | dos refresh con la misma medición → 1 fila |
| `IssRepositoryTest.refresh_borraLoAnteriorA24Horas` | retención de 24 h |
| `MigrationTest.migrar2a3_agregaLaTablaIssYConservaLosRecordatorios` | la migración no toca `reminders` |

**MockWebServer en el emulador.** El servidor escucha en `http://localhost`, y la política de la
etapa 1 bloquea HTTP. En vez de `usesCleartextTraffic`, que abriría HTTP para todo, se agregó un
`network_security_config.xml` **solo en `src/debug`** que permite HTTP únicamente hacia
`localhost` y `127.0.0.1`. El APK de release no lo contiene.

### Verificación

| Comando | Resultado |
|---|---|
| `./gradlew assembleDebug assembleRelease` | OK |
| `./gradlew testDebugUnitTest` | OK |
| `./gradlew connectedDebugAndroidTest` | 31/31 OK |
| `./gradlew lint` | 0 errores y 22 advertencias, las mismas de la etapa 2 |

## Criterios de aceptación

- [x] Sin red, la pantalla nunca queda en blanco si hay al menos una posición guardada
  (experimento y `conDatosGuardados_unErrorNoOcultaLosDatos`).
- [x] Un refresh repetido con el mismo timestamp no duplica filas
  (`refreshRepetidoConElMismoTimestamp_noDuplicaFilas` y `mismaMedicionDosVeces_noDuplicaFilas`).
