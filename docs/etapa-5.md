# Etapa 5 · Aplicar las preferencias en la app

**Objetivo:** que las preferencias cambien de verdad la experiencia, no solo la pantalla de configuración.

**Qué se ve en la app:** el tema oscuro cambia los colores de **toda** la app al instante. El
listado oculta los completados y se reordena por fecha o por prioridad. Todo eso sobrevive a
forzar la detención de la app.

## Qué se construyó

| Archivo | Rol |
|---|---|
| `MainActivity.kt` | observa `preferences.darkModeEnabled` y lo pasa a `ResumenModulos1y2Theme(darkTheme = …)` |
| `ui/recordatorios/AplicarPreferencias.kt` | función pura `aplicarPreferencias(lista, prefs)` |
| `ui/recordatorios/RecordatoriosViewModel.kt` | `combine(recordatorios, repository.preferences) { … }.stateIn(…)` y su `Factory` |
| `test/.../AplicarPreferenciasTest.kt` | 5 pruebas: las 4 combinaciones de preferencias y que la lista original no cambia |
| `gradle/libs.versions.toml` | DataStore actualizado a 1.3.0 (misma API) |

El tema del proyecto se llama `ResumenModulos1y2Theme` y cumple el papel de `RecordatoriosTheme`
de la consigna. El modelo `Recordatorio` ya traía `fecha` y `completado` desde la etapa 0, así que
no hubo que agregarlos.

```
_recordatorios (memoria) ──┐
                           ├─ combine ─► aplicarPreferencias ─► stateIn ─► StateFlow<List<Recordatorio>> ─► LazyColumn
repository.preferences ────┘
```

## Conceptos

1. **`combine` de dos flujos.** Cada vez que **cualquiera** de los dos emite (se marca un
   recordatorio o cambia una preferencia), se recalcula la lista visible con el último valor de
   ambos. Por eso, al volver de Configuración, el listado ya está reordenado sin código extra.
2. **Una sola fuente de verdad para el tema.** El tema no se guarda en el ViewModel ni en la
   pantalla: se lee de DataStore en el nivel más alto (`MainActivity`) y baja a toda la app por
   `MaterialTheme`. Configuración solo escribe; nunca cambia colores por su cuenta.
3. **Lógica fuera de los composables.** Filtrar y ordenar es una función pura: se prueba con JUnit
   y la pantalla solo dibuja la lista que recibe.
4. **Orden explícito.** El orden por prioridad usa un peso explícito (Alta = 0, Media = 1,
   Baja = 2) y desempata por fecha y luego por id, así el resultado es siempre el mismo.

> Nota para el docente: con `darkModeEnabled = false` por defecto, la app queda en tema claro
> aunque el sistema esté en oscuro. Una extensión natural es un tercer valor, "según el sistema".

## Guion para la clase (≈ 30 min)

1. Abrir Recordatorios (orden por fecha, con completados). Ir a ⚙, elegir *Prioridad* y volver:
   ya está reordenado. Preguntar *¿quién le avisó a la lista?* (Respuesta: `combine`.)
2. Activar *Tema oscuro*: cambia toda la app, incluida la pantalla actual.
3. Configuración → Aplicaciones → Forzar detención (o `adb shell am force-stop
   com.example.resumen_modulos1y2`). Reabrir: tema, filtro y orden se conservan. Marcar que los
   **recordatorios** marcados a mano vuelven a su estado inicial: siguen en memoria (Room es el
   tópico siguiente).
4. Ejecutar `AplicarPreferenciasTest` y relacionar cada prueba con una combinación de la pantalla.

## Evidencia (emulador, API 36, 19-11-2026)

Instalación limpia (`pm clear`). Luego, en Configuración: tema oscuro **activado**, mostrar
completados **desactivado** y orden **Prioridad**.

Archivo `files/datastore/user_preferences.preferences_pb` (72 bytes) tras los cambios
(`adb exec-out run-as com.example.resumen_modulos1y2 cat files/datastore/user_preferences.preferences_pb | od -c`):

```
\n 017 \n \t d a r k _ m o d e 022 002 \b 001        → dark_mode = true
\n 024 \n 016 s h o w _ c o m p l e t e d ... \b \0  → show_completed = false
\n 037 \n 016 r e m i n d e r _ o r d e r ... B Y _ P R I O R I T Y
```

| # | Situación | Tema | Completados | Orden | Conclusión |
|---|---|---|---|---|---|
| 0 | Primera apertura (sin archivo) | claro | visibles | fecha | sin datos → **todo por defecto** |
| 1 | Volver de Configuración ([captura](evidencia/1-configuracion.png)) | oscuro | ocultos | prioridad | `combine` reordena sin acciones extra |
| 2 | Rotar con el listado abierto | oscuro | ocultos | prioridad | no se pierde ni se repite nada |
| 3 | **Forzar detención** y reabrir ([captura](evidencia/2-reabierta-tras-force-stop.png)) | oscuro | ocultos | prioridad | **los tres valores se conservaron** |
| 4 | Se reemplazó `BY_PRIORITY` por el texto inválido `BY_TITLE___` en el archivo ([captura](evidencia/3-orden-invalido.png)) | oscuro | ocultos | **fecha** | dato inválido → **solo el orden** usó el valor por defecto; el resto se conservó |
| 5 | Se reemplazó el archivo por bytes basura ([captura](evidencia/4-archivo-danado.png)) | **claro** | **visibles** | **fecha** | la lectura falla (`CorruptionException`, que es una `IOException`) → `catch` emite `emptyPreferences()` → **todo por defecto, sin caerse** |

Los casos 4 y 5 se prepararon con la app detenida, escribiendo el archivo con
`adb shell "run-as com.example.resumen_modulos1y2 sh -c 'cat > files/datastore/user_preferences.preferences_pb'" < archivo.pb`.
Al terminar se restauró el archivo original.

## Criterios de aceptación

- [x] Al cambiar el orden en Configuración y volver, el listado ya está reordenado (fila 1).
- [x] Tras forzar la detención y reabrir, se conservan el tema, el filtro y el orden (fila 3).
- [x] `AplicarPreferenciasTest`: 5 tests, 0 failures.
