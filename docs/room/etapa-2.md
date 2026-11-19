# Room · Etapa 2 · DAO y base de datos

**Objetivo:** declarar las operaciones sobre la tabla y crear una única base de datos para la app.

**Qué se ve en la app:** todavía nada (la pantalla se conecta en la etapa 3). En clase se muestran
las pruebas del DAO corriendo en el emulador y el esquema exportado.

## Qué se construyó

| Archivo | Rol |
|---|---|
| `data/local/ReminderDao.kt` | consultas con `Flow`, filtro con parámetro, `@Upsert`, `UPDATE` parcial y `@Delete` |
| `data/local/AppDatabase.kt` | `@Database(entities = [ReminderEntity::class], version = 1, exportSchema = true)` + `@TypeConverters` |
| `di/AppContainer.kt` | `val appDatabase by lazy { Room.databaseBuilder(...).build() }`: una sola instancia |
| `app/schemas/.../AppDatabase/1.json` | esquema de la versión 1, generado por Room (se sube al repositorio) |
| `androidTest/.../ReminderDaoTest.kt` | 5 pruebas con `Room.inMemoryDatabaseBuilder` + `runTest` |

## Conceptos

1. **DAO = interfaz + SQL.** Se escriben las firmas y las consultas, y Room genera la
   implementación al compilar (KSP).
2. **Lecturas con `Flow`, escrituras con `suspend`.** `observeAll()` no lee una vez: emite una
   lista nueva **cada vez que la tabla cambia**. Las escrituras tocan el disco, así que son
   `suspend` para no bloquear la interfaz.
3. **Orden por prioridad real.** `ORDER BY priority` sería alfabético (Alta, Baja, Media). El
   `CASE` convierte cada texto en un peso. `due_at IS NULL` deja al final los recordatorios sin
   fecha (en SQLite, `NULL` va primero en un orden ascendente).
4. **Parámetros (`:completed`, `:id`).** Room los enlaza de forma segura: el valor nunca se pega
   como texto dentro del SQL, así que no hay inyección SQL.
5. **`@Upsert`.** Inserta si el id no existe y actualiza si existe. Guardar dos veces el mismo
   recordatorio no lo duplica.
6. **`UPDATE` parcial.** `updateCompleted` toca solo dos columnas, sin leer ni reescribir la fila.
7. **Una única instancia.** Abrir la base es costoso y dos instancias sobre el mismo archivo
   pueden pisarse (la misma regla que DataStore). Por eso se crea una vez, en `AppContainer`.
8. **`exportSchema = true`.** El JSON de `app/schemas/` es la "foto" de cada versión del esquema.
   Se versiona con git y lo necesitan las pruebas de migración (etapa 4).

## Experimento guiado: una columna mal escrita no compila

> Se hace en vivo y se deshace al terminar. No queda en el repositorio.

En `ReminderDao.kt`, cambiar `updated_at` por `updated_on` en `updateCompleted`:

```kotlin
@Query("UPDATE reminders SET completed = :completed, updated_on = :updatedAt WHERE id = :id")
```

Compilar (`Build → Make Project`). El build **falla** antes de instalar nada:

```
e: [ksp] .../data/local/ReminderDao.kt:50: There is a problem with the query:
   [SQLITE_ERROR] SQL error or missing database (no such column: updated_on)
```

**Por qué importa.** Room valida cada `@Query` contra el esquema de las entidades al compilar. Un
error de tipeo que con SQLite "a mano" aparecería recién cuando el usuario toca el botón (y
cerraría la app), aquí ni siquiera deja generar el APK. Probar también con una tabla mal escrita
(`FROM reminder`) o un parámetro que no existe en la firma (`:fecha`).

## Guion para la clase (≈ 35 min)

1. Construir el DAO de a una función. Para cada una, preguntar: *¿lee o escribe? ¿una vez o
   siempre?* De ahí sale si es `suspend` o devuelve `Flow`.
2. Escribir primero `ORDER BY priority` y preguntar qué orden produce. Ejecutar
   `guardarYLeer_ordenaPorPrioridadRealYLuegoPorFecha` y ver cómo falla. Luego agregar el `CASE`.
3. Experimento guiado (arriba).
4. Ejecutar `ReminderDaoTest` en el emulador y abrir `app/schemas/.../1.json` para comparar el
   `createSql` con la entidad.

## Pruebas (`androidTest`, base en memoria)

| Prueba | Qué demuestra |
|---|---|
| `guardarYLeer_ordenaPorPrioridadRealYLuegoPorFecha` | `save` + `observeAll().first()`; orden Alta → Media → Baja, y sin fecha al final |
| `saveDosVecesConElMismoId_noDuplica` | `@Upsert` actualiza en vez de duplicar |
| `updateCompleted_cambiaSoloEseCampo` | la fila leída es la original con solo `completed` y `updated_at` cambiados |
| `filtroPorCompletado_devuelveSoloLosEsperados` | `observeByCompleted(false/true)` |
| `observeByIdInexistente_devuelveNull` | un id que no existe produce `null`, no una excepción |

Comando:
`./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.resumen_modulos1y2.data.local.ReminderDaoTest`
→ 5 tests, 0 failures.

## Criterios de aceptación

- [x] Las cinco pruebas pasan.
- [x] Una columna mal escrita en un `@Query` falla al compilar (experimento guiado, con el error real).
