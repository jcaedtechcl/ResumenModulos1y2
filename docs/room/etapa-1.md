# Room · Etapa 1 · La entidad

**Objetivo:** describir en Kotlin la tabla donde se guardarán los recordatorios y separar ese
formato de almacenamiento del modelo que usa la app.

**Qué se ve en la app:** nada cambia. Los recordatorios siguen en memoria; esta etapa solo define
la tabla. En clase se muestran el código y las pruebas de los mapeadores.

## Qué se construyó

| Archivo | Rol |
|---|---|
| `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts` | Room 2.8.5 + KSP; `room { schemaDirectory(...) }` |
| `data/local/ReminderEntity.kt` | `@Entity(tableName = "reminders")` |
| `data/local/Converters.kt` | `@TypeConverter` Prioridad ↔ String |
| `data/local/ReminderMappers.kt` | `ReminderEntity.toDomain()` y `Recordatorio.toEntity(updatedAt)` |
| `model/Recordatorio.kt` | dominio ampliado: `id: String`, `descripcion?`, `categoria`, `fecha?` |
| `test/.../ReminderMappersTest.kt` | 3 pruebas JUnit puras |

La tabla que Room genera a partir de la entidad (ver `app/schemas/` a partir de la etapa 2):

```sql
CREATE TABLE reminders (
  id          TEXT    NOT NULL PRIMARY KEY,  -- UUID
  title       TEXT    NOT NULL,
  description TEXT,                          -- String? → acepta NULL
  category    TEXT    NOT NULL,
  priority    TEXT    NOT NULL,              -- "Alta" | "Media" | "Baja" (TypeConverter)
  completed   INTEGER NOT NULL,              -- Boolean → 0/1
  due_at      INTEGER,                       -- @ColumnInfo(name = "due_at"), milisegundos
  updated_at  INTEGER NOT NULL
)
```

## Conceptos

1. **Entidad = fila.** Cada propiedad de `ReminderEntity` es una columna. Una propiedad nullable
   genera una columna que acepta `NULL`; una no nullable, `NOT NULL`.
2. **Dos modelos, dos responsabilidades.** `ReminderEntity` habla el idioma de la base: nombres
   de la lectura, `snake_case` y fechas como números. `Recordatorio` habla el idioma de la app:
   `LocalDate`, nombres en español. Los mapeadores traducen en un solo lugar. Si mañana cambia la
   tabla, las pantallas no se enteran.
3. **`@ColumnInfo`.** Separa el nombre de Kotlin (`dueAt`) del nombre de la columna (`due_at`).
4. **UUID como clave primaria.** El `id` se genera al crear el recordatorio
   (`UUID.randomUUID().toString()`, etapa 3), no al insertarlo. Así el objeto tiene identidad
   antes de llegar a la base, y dos dispositivos nunca generarían el mismo id.
5. **`@TypeConverter`.** SQLite solo guarda números, texto y bytes. El converter guarda
   `prioridad.name` y no `ordinal`, porque reordenar el enum cambiaría el significado de los
   datos ya guardados. Al leer usa `entries.firstOrNull { … } ?: Media`, la misma defensa que
   `ReminderOrder` en DataStore: un texto desconocido no cierra la app. (Room tiene su propio
   conversor de enums, pero lanza una excepción con valores desconocidos.)
6. **`updatedAt` es de almacenamiento.** El dominio no lo necesita, por eso `toEntity` lo recibe
   como parámetro y el repositorio pondrá la hora actual al guardar.
7. **Fechas en UTC.** `LocalDate` → milisegundos del inicio del día en UTC, y de vuelta con UTC.
   Usar la misma zona en ambos sentidos garantiza que una fecha no "se corra" un día si el
   teléfono cambia de zona horaria.

## Guion para la clase (≈ 25 min)

1. Escribir el `CREATE TABLE` de arriba en la pizarra y construir la entidad columna por columna.
2. Preguntar: *¿por qué `description` es `String?` y `title` no?* Relacionarlo con `NULL` y
   `NOT NULL`.
3. Mostrar el converter con `valueOf` y preguntar qué pasa si en la base dice `"URGENTE"`.
   Ejecutar `converter_guardaElNombre_yLeeUnoDesconocidoComoMedia` con `valueOf` (cambio
   temporal): falla con `IllegalArgumentException`. Deshacer el cambio.
4. Ejecutar `ReminderMappersTest`: una ida y vuelta entidad ↔ dominio conserva todos los campos.

## Criterios de aceptación

- [x] La UI sigue usando `Recordatorio`; ninguna pantalla importa `ReminderEntity`:
  `grep -rl "ReminderEntity\|data.local" app/src/main/java/com/example/resumen_modulos1y2/ui` no
  devuelve resultados.
- [x] `ReminderMappersTest`: 3 tests, 0 failures.
- [x] `data/preferences/` (DataStore) no se modificó.
