# Módulo 3 · Persistencia de recordatorios con Room

Guía docente de la consigna 2 de la Actividad 3 (U1 · Tema 2). Antes, los recordatorios vivían en
memoria y desaparecían al cerrar la app. Al final de estas etapas se guardan en una base de datos
SQLite local y la pantalla se actualiza sola cuando cambian.

Cada etapa es un commit con un **tag anotado**. Para mostrar una etapa en clase:

```bash
git tag -l "m3-room-*" -n1      # lista las etapas con su descripción
git checkout m3-room-etapa-2    # deja el proyecto como quedó al final de la etapa 2
```

| Etapa | Tag | Concepto central | Guía |
|---|---|---|---|
| 1 | `m3-room-etapa-1` | La entidad: tabla, columnas, TypeConverter y mapeadores | [etapa-1.md](etapa-1.md) |
| 2 | `m3-room-etapa-2` | DAO (Flow, `@Upsert`, `UPDATE`, `@Delete`) y `AppDatabase` única | [etapa-2.md](etapa-2.md) |
| 3 | `m3-room-etapa-3` | Repositorio, ViewModel con `stateIn` y pantalla que se actualiza sola | [etapa-3.md](etapa-3.md) |

## Punto de partida (decisiones documentadas)

- **Proyecto.** Se trabaja sobre el proyecto existente, que ya tiene Recordatorios con
  preferencias en DataStore (ver [../README.md](../README.md)). Su paquete es
  `com.example.resumen_modulos1y2`, no `cl.ipp.recordatorios` como dice la consigna. No se
  renombró, porque cambiaría todos los archivos y rompería el historial de los módulos anteriores.
- **DataStore no se modificó.** `data/preferences/` queda igual: las preferencias (tema, filtro,
  orden) siguen en DataStore y los recordatorios pasan a Room. Cada herramienta guarda lo que le
  corresponde.
- **Modelo de dominio ampliado.** Para cumplir con la entidad de la lectura, `Recordatorio` ganó
  `descripcion?` y `categoria`. Además, `id` pasó de `Long` a `String` (UUID) y `fecha` ahora
  acepta `null` (recordatorios sin fecha). La UI sigue usando `Recordatorio`.

## Versiones

| Herramienta | Versión | Nota |
|---|---|---|
| Room (`room-runtime`, `room-ktx`, `room-compiler`, `room-testing`) | **2.8.5** | La consigna pide 2.8.4 "o la estable vigente". Al momento de hacer esto, la estable vigente es 2.8.5, una versión de corrección de errores con la misma API. |
| KSP | **2.3.14** | El proyecto usa AGP 9, que trae Kotlin integrado. Las versiones de KSP con formato `2.2.10-2.0.x` fallan con *"Using kotlin.sourceSets DSL … is not allowed with built-in Kotlin"*; la serie 2.3.x es la que soporta AGP 9. |
| Plugin `androidx.room` | 2.8.5 | Configura `room { schemaDirectory("$projectDir/schemas") }`: exporta el esquema a `app/schemas/`. |

## Alcance

- **Sí:** Room con KSP, Flow, coroutines, StateFlow, ViewModel e inyección de dependencias manual
  (`AppContainer` en `RecordatoriosApp`).
- **No:** Retrofit, sincronización remota, Hilt ni WorkManager.
