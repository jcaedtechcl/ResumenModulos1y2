# Módulo 3 · Preferencias persistentes con Preferences DataStore

Guía docente de la app **Recordatorios**: el tema, el filtro y el orden del listado dejan de
perderse al cerrar la app. El trabajo se divide en etapas de complejidad creciente; cada una
compila, se ejecuta y agrega **un solo concepto central** sobre la anterior.

## Punto de partida (decisión documentada)

El proyecto existente (`ResumenModulos1y2`) ya compilaba y cubría los Módulos 1 y 2: una app de
tareas con Material 3, `Scaffold`, `LazyColumn`, ViewModels con `StateFlow` y Navigation Compose
con rutas `@Serializable`. Lo que **no** tenía era el dominio de Recordatorios.

Por eso no se creó una app nueva: se agregó Recordatorios como una sección más del mismo
proyecto (menú ⋮ → *Recordatorios · DataStore*), reutilizando lo que los estudiantes ya conocen:

| Se reutiliza | Dónde |
|---|---|
| `Prioridad` (Alta, Media, Baja) | se movió de `ui/componentes` a `model/Prioridad.kt` para que el modelo no dependa de la UI |
| `ChipPrioridad`, `BarraLeccion` | `ui/componentes/` |
| Tema (`ResumenModulos1y2Theme`) | `ui/theme/Theme.kt`; cumple el papel de `RecordatoriosTheme` de la consigna |
| NavHost con rutas tipadas | `navegacion/AppNavHost.kt`, `navegacion/Rutas.kt` |

La base mínima de la etapa 0 es:

- `model/Recordatorio.kt`: `Recordatorio(id, titulo, fecha, prioridad, completado)` y datos de
  ejemplo elegidos para que ordenar por fecha y por prioridad dé resultados distintos.
- `ui/recordatorios/RecordatoriosViewModel.kt`: la lista **en memoria** en un `MutableStateFlow`.
- `ui/recordatorios/RecordatoriosScreen.kt`: `Scaffold` + `TopAppBar` + `LazyColumn`.

`fecha` es un `java.time.LocalDate`. Como el `minSdk` es 24 y `java.time` llega en la API 26, se
activó *core library desugaring* en `app/build.gradle.kts`.

## Alcance

- **Sí:** Preferences DataStore, Flow, coroutines, StateFlow, ViewModel e inyección de
  dependencias manual (un contenedor en la clase `Application`).
- **No:** Room, Retrofit, Hilt, Proto DataStore ni SharedPreferences.
- Los **recordatorios siguen en memoria**: persistirlos es el tópico siguiente (Room). Aquí solo
  se persisten preferencias.

## Nombres

Las clases de preferencias usan los nombres de la lectura del módulo para que los estudiantes las
reconozcan: `UserPreferences`, `PreferenceKeys`, `UserPreferencesRepository`, `ReminderOrder`.
El dominio de la app (`Recordatorio`, `Prioridad`) conserva sus nombres en español.

## Etapas

Cada etapa es un commit en la rama `50-modulo-3-Persistent-PReferences`. Para mostrar una etapa
en clase: `git log --oneline` y `git checkout <commit>`.

| Etapa | Concepto central | Guía |
|---|---|---|
| 0 | Base: Recordatorios en memoria (se pierde todo al cerrar) | este archivo |
| 1 | Modelo propio, claves tipadas y una única instancia de DataStore | [etapa-1.md](etapa-1.md) |
