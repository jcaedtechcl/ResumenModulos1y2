# Room · Etapa 3 · Repositorio, ViewModel y pantalla

**Objetivo:** que la pantalla muestre lo que hay en la base de datos y se actualice sola, sin
código de "refrescar".

**Qué se ve en la app:** menú ⋮ → *Recordatorios · DataStore*. El botón **+** abre un formulario
(título, descripción, categoría, prioridad y vencimiento). Cada fila se puede marcar o eliminar, y
los chips **Todos / Pendientes / Completados** filtran. Todo queda guardado: al cerrar la app y
reabrirla, los recordatorios siguen ahí.

## Qué se construyó

| Archivo | Rol |
|---|---|
| `data/ReminderRepository.kt` | único punto de acceso: `Flow<List<Recordatorio>>` ya mapeado y `suspend fun guardar / completar / eliminar` |
| `di/AppContainer.kt` | la base pasa a ser privada; se expone `reminderRepository` |
| `ui/recordatorios/RecordatoriosViewModel.kt` | la lista en memoria se reemplaza por el `Flow` del repositorio + `stateIn` |
| `ui/recordatorios/RecordatoriosScreen.kt` | FAB, chips de filtro, eliminar por fila |
| `ui/recordatorios/FormularioRecordatorio.kt` | diálogo de alta con `rememberSaveable` |

```
Compose ──eventos──► RecordatoriosViewModel ──► ReminderRepository ──► ReminderDao ──► SQLite
   ▲                        │ stateIn                  │ map toDomain()      │ Flow
   └────── StateFlow ◄──────┴──────────────────────────┴─────────────────────┘
            (cada cambio en la tabla vuelve a emitir y la pantalla se recompone)
```

## Conceptos

1. **Una sola fuente de verdad.** La lista ya no vive en el ViewModel: vive en la base. El
   ViewModel **observa** (`repository.observeAll()`) y **pide cambios**
   (`viewModelScope.launch { repository.guardar(...) }`). Nunca modifica la lista a mano.
2. **La pantalla se actualiza sola.** Al guardar, marcar o eliminar, Room avisa al `Flow` del DAO,
   que emite la lista nueva. El `StateFlow` cambia y `collectAsStateWithLifecycle()` recompone.
   No existe una función "recargar".
3. **El repositorio traduce.** Devuelve `Recordatorio`, no `ReminderEntity`, y pone `updatedAt`
   con la hora actual. El ViewModel no sabe que existe Room.
4. **`stateIn(viewModelScope, WhileSubscribed(5_000), emptyList())`.** Convierte el Flow frío en
   estado para la pantalla. Una rotación no reinicia la consulta; salir de la pantalla sí la
   detiene a los 5 s.
5. **Filtro con `flatMapLatest`.** Cada chip usa una consulta distinta del DAO (`observeAll` u
   `observeByCompleted`). Al cambiar de chip, `flatMapLatest` deja de observar la consulta anterior
   y pasa a la nueva.
6. **Convivencia con DataStore.** Las preferencias de la actividad anterior se siguen aplicando con
   `combine` + `aplicarPreferencias`: la base entrega los datos y las preferencias deciden el orden
   y si se ven los completados. Si el usuario elige un chip explícito (Pendientes o Completados),
   ese filtro manda sobre "Mostrar completados".
7. **UUID al crear.** `agregar()` genera `UUID.randomUUID().toString()` antes de guardar.

## Guion para la clase (≈ 40 min)

1. Mostrar el ViewModel de la actividad anterior (`MutableStateFlow(recordatoriosDeEjemplo())`) y
   el nuevo. Preguntar: *¿dónde está ahora la lista?*
2. Crear tres recordatorios, marcar uno y eliminar otro. Señalar que el código de la pantalla no
   tiene ninguna llamada a "refrescar".
3. Forzar la detención (Configuración → Aplicaciones → Forzar detención, o
   `adb shell am force-stop com.example.resumen_modulos1y2`) y reabrir. Los recordatorios siguen
   ahí, con su estado.
4. Abrir **App Inspection → Database Inspector** en Android Studio con la app en ejecución.
   Ejecutar `SELECT * FROM reminders` y ver las columnas `due_at` y `updated_at` como números.
   Con *Live updates* activado, marcar un recordatorio en el emulador y ver cambiar la fila.

## Evidencia (emulador, 19-11-2026)

Instalación limpia y, desde la interfaz: tres altas, marcar "Comprar leche", filtros y eliminar
"Llamar a mama".

| Paso | Listado observado |
|---|---|
| Tres altas (orden por fecha: hoy, en una semana, sin fecha) | Comprar leche · Pagar arriendo · Llamar a mama |
| Chip *Pendientes* | Pagar arriendo · Llamar a mama |
| Chip *Completados* | Comprar leche |
| Eliminar "Llamar a mama" | Comprar leche · Pagar arriendo |
| **Forzar detención y reabrir** ([captura](evidencia/reabierta-tras-force-stop.png)) | Comprar leche (marcado) · Pagar arriendo |

Archivos en el dispositivo: `databases/recordatorios.db` (+ `-wal` y `-shm`, el registro de
escritura de SQLite).

## Criterios de aceptación

- [x] Ningún composable accede al DAO ni a `ReminderEntity`. El orden es Compose → ViewModel →
  Repositorio → DAO. Verificación:
  `grep -rnE "^import .*(data\.local|ReminderRepository)" app/src/main/java/com/example/resumen_modulos1y2/ui`
  solo encuentra el import del repositorio en `RecordatoriosViewModel.kt`.
- [x] Al forzar la detención y reabrir, los recordatorios siguen ahí (evidencia).
- [x] Cada alta, marcado y eliminación se refleja al instante, sin código de refresco.
