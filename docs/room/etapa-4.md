# Room · Etapa 4 · Migración y evidencia

**Objetivo:** cambiar el esquema de una base que ya tiene datos de usuarios sin perder nada.

**Qué se ve en la app:** nada distinto, y eso es lo importante. Quien actualiza la app conserva
todos sus recordatorios aunque la tabla tenga una columna nueva.

## Qué se construyó

| Archivo | Rol |
|---|---|
| `data/local/ReminderEntity.kt` | `@ColumnInfo(name = "archived", defaultValue = "0") val archived: Boolean = false` |
| `data/local/AppDatabase.kt` | `version = 2` |
| `data/local/Migrations.kt` | `MIGRATION_1_2`: `ALTER TABLE reminders ADD COLUMN archived INTEGER NOT NULL DEFAULT 0` |
| `di/AppContainer.kt` | `.addMigrations(MIGRATION_1_2)`, **sin** `fallbackToDestructiveMigration()` (con un comentario que explica por qué) |
| `app/schemas/.../2.json` | esquema de la versión 2; `1.json` se conserva |
| `androidTest/.../MigrationTest.kt` | 2 pruebas con `MigrationTestHelper` |
| `model/Recordatorio.kt`, `ReminderMappers.kt` | `archivado` viaja en los mapeadores; si no, guardar un recordatorio lo "desarchivaría" |

## Conceptos

1. **El número de versión es un contrato.** Al abrir la base, Room compara la versión guardada
   (`PRAGMA user_version`) con la de `@Database`. Si son distintas, busca una migración que lleve
   de una a otra. Si no la encuentra, la app falla con `IllegalStateException`.
2. **Migración manual = SQL explícito.** `ALTER TABLE … ADD COLUMN` agrega la columna sin tocar
   las filas. `DEFAULT 0` rellena las filas existentes.
3. **El esquema debe coincidir.** Después de migrar, Room valida que la tabla sea idéntica a la
   esperada por la entidad. Por eso la entidad declara `defaultValue = "0"`: sin eso, la
   validación detecta la diferencia en el valor por defecto y la app falla al abrir la base.
4. **Por qué no `fallbackToDestructiveMigration()`.** Con esa opción, si falta una migración,
   Room **borra la base y la crea vacía**. Los recordatorios del usuario desaparecerían en silencio
   al actualizar. Sin ella, el olvido es un error visible durante el desarrollo, que es cuando hay
   que descubrirlo.
5. **Los esquemas exportados son la historia.** `MigrationTestHelper` crea la base "como era" en
   la versión 1 leyendo `1.json`. Por eso los JSON se suben al repositorio y nunca se borran.

## Guion para la clase (≈ 40 min)

1. Con la app de la etapa 3 instalada y con datos, agregar `archived` a la entidad y subir a
   `version = 2` **sin** migración. Ejecutar sobre los datos existentes: la app se cae con
   *"A migration from 1 to 2 was required but not found"*. Preguntar: *¿qué le pasaría a un
   usuario real?*
2. Agregar `fallbackToDestructiveMigration()` y ejecutar: ya no se cae, pero **la lista está
   vacía**. Leer en voz alta el comentario de `AppContainer`. Quitarlo.
3. Escribir `MIGRATION_1_2` y registrarla con `addMigrations`. Ejecutar: los datos siguen ahí.
4. Ejecutar `MigrationTest` y abrir `1.json` y `2.json` lado a lado para comparar el `createSql`.

## Evidencia (emulador, 19-11-2026)

### Pruebas automáticas

`./gradlew :app:connectedDebugAndroidTest` → **21 tests, 0 failures**, entre ellas:

| Prueba | Qué demuestra |
|---|---|
| `migrar1a2_conservaLosDatos` | crea la v1 con 2 filas, migra con `runMigrationsAndValidate(…, 2, true, MIGRATION_1_2)` y verifica fila por fila: datos intactos y `archived = 0` |
| `migrar1a2_laAppLeeLosDatosConElDao` | después de migrar, la base se abre como lo hace la app (`Room.databaseBuilder(...).addMigrations(...)`) y el DAO lee el recordatorio |

### Actualización real en el dispositivo

1. Se instaló el APK del tag `m3-room-etapa-3` (base **v1**), se crearon dos recordatorios desde
   la interfaz y se marcó uno.
2. Se instaló encima el APK de esta etapa (base **v2**) con `adb install -r`, que conserva los
   datos, como una actualización desde Play Store.
3. Se copió la base antes y después (`run-as … cat databases/recordatorios.db*`) y se leyó con
   `sqlite3`:

```
== v1 (antes de actualizar)
  user_version = 1
  columnas = ['id', 'title', 'description', 'category', 'priority', 'completed', 'due_at', 'updated_at']
   ('5b96599a-…', 'Comprar leche',  None, 'General', 'Baja', 1, 1795046400000, 1795119861755)
   ('673e9ea5-…', 'Pagar arriendo', None, 'General', 'Alta', 0, 1795132800000, 1795119840385)

== v2 (después de actualizar)
  user_version = 2
  columnas = [..., 'updated_at', 'archived']
   ('5b96599a-…', 'Comprar leche',  None, 'General', 'Baja', 1, 1795046400000, 1795119861755, 0)
   ('673e9ea5-…', 'Pagar arriendo', None, 'General', 'Alta', 0, 1795132800000, 1795119840385, 0)
```

Los mismos UUID, los mismos valores y la columna nueva en `0`. La app muestra ambos
recordatorios, con "Comprar leche" todavía marcado ([captura](evidencia/tras-migracion-v2.png)).

## Nota técnica: kotlinx-serialization

`MigrationTestHelper` lee los esquemas JSON con `kotlinx-serialization` 1.8.1. Navigation Compose
trae la 1.7.3 y el APK de pruebas debe usar las mismas versiones que la app, así que la prueba
fallaba con `AbstractMethodError … typeParametersSerializers()`. Se declaró
`kotlinx-serialization-core` 1.8.1 en `app/build.gradle.kts` (con un comentario que lo explica).

## Criterios de aceptación

- [x] Columna `archived: Boolean = false`, `version = 2` y `MIGRATION_1_2` manual con el SQL pedido.
- [x] Prueba con `MigrationTestHelper`: crea la v1 con datos, migra a la v2 y verifica que se conservan.
- [x] No se usa `fallbackToDestructiveMigration()`; el comentario en `AppContainer` explica por qué.
