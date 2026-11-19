# Etapa 2 · Leer como Flow, con valores por defecto seguros

**Objetivo:** exponer las preferencias como un flujo observable que nunca entrega un estado inválido.

**Qué se ve en la app:** todavía nada. Lo que se muestra en clase son las pruebas unitarias en
verde y la tabla del experimento.

## Qué se construyó

| Archivo | Rol |
|---|---|
| `data/preferences/PreferencesMapper.kt` | función pura `Preferences.toUserPreferences()` |
| `data/preferences/UserPreferencesRepository.kt` | `val preferences: Flow<UserPreferences>` con `catch` y `map` |
| `di/AppContainer.kt` | ahora expone el repositorio, no el DataStore |
| `test/.../PreferencesMapperTest.kt` | 4 pruebas con `mutablePreferencesOf(...)` |

```
dataStore.data ──► catch { IOException → emptyPreferences() } ──► map { toUserPreferences() } ──► Flow<UserPreferences>
 Flow<Preferences>                                                                                 (nunca inválido)
```

## Conceptos

1. **`Flow<T>` y genéricos.** `Flow<Preferences>` y `Flow<UserPreferences>` son el mismo tipo de
   flujo con distinto contenido. `map` convierte uno en el otro sin cambiar la "tubería".
2. **Flujo frío.** Declarar `preferences` no lee el disco. La lectura empieza cuando alguien lo
   recolecta (etapa 4) y el flujo vuelve a emitir cada vez que el archivo cambia.
3. **`catch` antes de `map`.** `catch` solo atrapa errores de los operadores que están **más
   arriba**. Puesto antes de `map`, protege la lectura del archivo. Una `IOException` (archivo
   dañado o ilegible) se reemplaza por `emptyPreferences()`, y el mapper la convierte en los
   valores por defecto. Cualquier otro error se relanza: es un bug y no debe esconderse.
4. **Operador Elvis `?:`.** `prefs[DARK_MODE]` es `Boolean?` porque la clave puede no existir.
   `?: false` da el valor por defecto en una sola expresión.
5. **`firstOrNull` en vez de `valueOf`.** `ReminderOrder.valueOf("BY_TITLE")` lanza
   `IllegalArgumentException` y la app se cae al abrir. `entries.firstOrNull { it.name == guardado }`
   devuelve `null` y el Elvis lo convierte en `BY_DATE`.
6. **Funciones puras testeables.** El mapper no depende de Android ni de coroutines: entra un
   `Preferences`, sale un `UserPreferences`. Por eso se prueba en milisegundos con JUnit puro.

## Guion para la clase (≈ 25 min)

1. Escribir el mapper en la pizarra **sin** Elvis y preguntar qué tipo devuelve
   `this[PreferenceKeys.DARK_MODE]`. Que lleguen ellos a `Boolean?`.
2. Mostrar `valueOf` y preguntar qué pasa si en el archivo dice `"BY_TITLE"`. Ejecutar la prueba
   `ordenDesconocido_caeEnElPorDefecto_sinAfectarLoDemas` con `valueOf` en el mapper (cambio
   temporal) y ver cómo falla con `IllegalArgumentException`. Volver a `firstOrNull`.
3. Abrir el repositorio y mover `catch` **después** de `map` en vivo. Preguntar qué errores atrapa
   ahora (respuesta: también los del mapper, que deberían ser bugs visibles). Dejarlo como estaba.
4. Ejecutar `PreferencesMapperTest` (clic en el triángulo verde) y completar la tabla de abajo
   con el curso.

## Experimento guiado: valor guardado → valor que ve la app

Cada fila corresponde a una prueba de `PreferencesMapperTest`. Una celda vacía en "Guardado"
significa que la clave **no existe** en el archivo.

| Caso (prueba) | `dark_mode` guardado | `show_completed` guardado | `reminder_order` guardado | La app ve |
|---|---|---|---|---|
| Sin datos (`sinDatos_usaLosValoresPorDefecto`) | | | | `false`, `true`, `BY_DATE` (todo por defecto) |
| Todos los datos (`todosLosDatos_seLeenTalCual`) | `true` | `false` | `"BY_PRIORITY"` | `true`, `false`, `BY_PRIORITY` (todo conservado) |
| Orden desconocido (`ordenDesconocido_caeEnElPorDefecto_sinAfectarLoDemas`) | `true` | | `"BY_TITLE"` | `true` (conservado), `true` (por defecto), `BY_DATE` (**inválido → por defecto**) |
| Datos parciales (`datosParciales_completanLoQueFaltaConElPorDefecto`) | | `false` | | `false` (por defecto), `false` (conservado), `BY_DATE` (por defecto) |

Pregunta de cierre: *el orden desconocido no "contagia" a las otras claves. ¿Por qué?* (Cada clave
se resuelve por separado en el mapper.)

## Criterios de aceptación

- [x] El mapper no depende de Android ni de coroutines: `PreferencesMapperTest` está en
  `src/test` (JUnit puro).
- [x] Las cuatro pruebas pasan:
  `./gradlew :app:testDebugUnitTest --tests "*PreferencesMapperTest"` → 4 tests, 0 failures.
