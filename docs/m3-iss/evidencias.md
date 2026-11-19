# M3 ISS · Evidencias

Tabla para completar en clase o en la entrega. La columna **Observado** ya trae lo que se registró
en el emulador (Android 16, API 36) el 19-11-2026 durante el desarrollo. Al repetir la prueba en
otro equipo, se reemplaza por lo observado ahí y se agrega la captura en `evidencia/`.

| Escenario | Pasos | Resultado esperado | Observado | Captura |
|---|---|---|---|---|
| Consulta en línea | Abrir la pantalla con red | Posición actual y fila nueva en el historial | Posición con "Medida hace instantes" y fila nueva en *Historial reciente* (etapa 3) | [e1-consulta](evidencia/e1-consulta.png) · [e6-pantalla-completa](evidencia/e6-pantalla-completa.png) |
| Sin conexión | Modo avión, forzar detención, reabrir | Última posición con antigüedad y banner | Banner "Sin conexión: mostrando la última posición conocida", "Medida hace 1 min" y 2 filas en el historial | [e3-sin-conexion-tras-reabrir](evidencia/e3-sin-conexion-tras-reabrir.png) · [e4-banner](evidencia/e4-banner.png) |
| Vuelve la red | Desactivar el modo avión | Refresh automático, banner oculto | El banner desaparece y llega una posición nueva sin tocar nada; 3 ciclos de modo avión en ~18 s produjeron 1 sola solicitud | [e4-red-volvio](evidencia/e4-red-volvio.png) |
| Límite de la API | Tocar "Consultar" varias veces | Sin 429; los toques extra se ignoran | 8 toques en ~3 s después de una consulta → 0 solicitudes nuevas en Logcat | — (Logcat, etapa 2) |
| Error del servidor | Prueba con MockWebServer 500 | Mensaje recuperable, datos locales intactos | `respuesta500_esHttpErrorDelServidor` (→ `HttpError(500)`, reintentable) y `refreshFallido_noBorraLoAnterior` (la fila previa sigue ahí): ✔ | — (pruebas) |
| Trabajo en segundo plano | Forzar el worker con la app cerrada | Nueva posición al reabrir | Con la app cerrada (`am kill`), el sistema ejecutó el periódico a las 19:50:23 (`Worker result SUCCESS`); al reabrir, el historial tenía la fila de las 19:50:25, que la app no pidió | [e5-posicion-con-app-cerrada](evidencia/e5-posicion-con-app-cerrada.png) |
| Trabajo único | "Actualizar en segundo plano" ×5 | Un solo trabajo en el Background Task Inspector | 5 toques en 2 s → una sola fila `iss_sync_now` en el diagnóstico de WorkManager | [e5-en-cola-sin-red](evidencia/e5-en-cola-sin-red.png) |
| Restricción de red | Encolar con modo avión | Trabajo en espera hasta tener red | *Inmediata: En cola* con el modo avión; al desactivarlo (19:35:03) se ejecutó solo (`--> GET` 19:35:04) → *Completada · 19:35* | [e5-completado-al-volver-red](evidencia/e5-completado-al-volver-red.png) |
| Recordatorio de observación | Tocar "Crear recordatorio de observación" y abrir Recordatorios | Recordatorio "Observar la ISS" con la posición en la descripción | "Observar la ISS" · *Última posición conocida (19:51:38): latitud -25.4845°, longitud 49.4133°, altitud 430.4 km. En la sombra de la Tierra.* · categoría Observación; aparece en la lista sin recargar | [e6-recordatorio-creado](evidencia/e6-recordatorio-creado.png) |

## Cómo obtener cada evidencia

- **Solicitudes de red:** Logcat con el filtro `tag:okhttp.OkHttpClient` (solo en debug), o
  **App Inspection → Network Inspector**.
- **Trabajos de WorkManager:** **App Inspection → Background Task Inspector**, o por consola:

  ```
  adb shell am broadcast -a androidx.work.diagnostics.REQUEST_DIAGNOSTICS -p com.example.resumen_modulos1y2
  # y en Logcat: tag:WM-DiagnosticsWrkr
  ```

- **Base de datos:** **App Inspection → Database Inspector** → `recordatorios.db` →
  `iss_positions` (con *Live updates*).
- **Modo avión por consola:**
  `adb shell cmd connectivity airplane-mode enable` / `disable`.
- **Forzar la detención:** `adb shell am force-stop com.example.resumen_modulos1y2`.
  - Ojo: `force-stop` **también cancela los trabajos** de WorkManager hasta que la app se vuelve
    a abrir; WorkManager los reprograma al arrancar.
  - Para "cerrar la app" sin cancelar los trabajos: botón Inicio y luego
    `adb shell am kill com.example.resumen_modulos1y2`.

## Pruebas automáticas (resumen)

| Suite | Tipo | Pruebas |
|---|---|---|
| `SafeApiCallTest` | unitaria + MockWebServer | 8: 200 válido, JSON inválido, no JSON, 404, 429, 500, timeout y sin servidor |
| `IssViewModelTest` | unitaria | 10: frecuencia, vacío ≠ error, bucle, reconexión y recordatorio |
| `IssUiStateTest` | unitaria | 5: antigüedad y combinación de estados |
| `IssRepositoryConcurrenciaTest` | unitaria | 1: Mutex |
| `IssPositionDaoTest` | instrumentada, Room en memoria | 5 |
| `IssRepositoryTest` | instrumentada, Room en memoria + MockWebServer | 4 |
| `IssSyncWorkerTest` | instrumentada, `TestListenableWorkerBuilder` | 4 |
| `MigrationTest.migrar2a3_…` | instrumentada, `MigrationTestHelper` | 1 |
