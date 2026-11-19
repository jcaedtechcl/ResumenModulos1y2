# Etapa 4 · ViewModel y pantalla de Configuración

**Objetivo:** conectar DataStore con la interfaz siguiendo el flujo unidireccional de datos.

**Qué se ve en la app:** menú ⋮ → *Recordatorios · DataStore* → engranaje ⚙. Los interruptores y
el selector de orden **se guardan**: al cerrar y reabrir la app, la pantalla de Configuración
muestra lo último elegido. Todavía no cambian el tema ni el listado (eso es la etapa 5).

## Qué se construyó

| Archivo | Rol |
|---|---|
| `ui/configuracion/SettingsViewModel.kt` | `preferences: StateFlow<UserPreferences>` con `stateIn` y los eventos `onDarkModeChange`, `onShowCompletedChange` y `onOrderChange` |
| `RecordatoriosApp.kt` | `CreationExtras.appContainer()`, que usa la fábrica `viewModelFactory { initializer { … } }` |
| `ui/configuracion/SettingsScreen.kt` | `SettingsScreen` (con estado), `SettingsContent` (sin estado) y `@Preview` |
| `ui/componentes/ComponentesComunes.kt` | `BarraLeccion` gana un slot opcional `acciones` |
| `ui/recordatorios/RecordatoriosScreen.kt` | ícono ⚙ con `contentDescription` |
| `navegacion/Rutas.kt`, `AppNavHost.kt` | `RutaConfiguracion` en el grafo existente |
| `androidTest/.../SettingsContentTest.kt` | 3 pruebas de UI con `createComposeRule` |

```
                 estado ▼ (StateFlow<UserPreferences>)
Repositorio ──► SettingsViewModel ──► SettingsScreen ──► SettingsContent ──► Switch / SegmentedButton
     ▲                 ▲                                       │
     └── setDarkMode ◄─┴──── onDarkModeChange(true) ◄──────────┘  eventos ▲ (callbacks)
```

## Conceptos

1. **El estado baja, los eventos suben.** `SettingsContent` recibe un `UserPreferences` y
   funciones. No guarda nada: cuando el usuario toca, **avisa**. El ViewModel escribe en
   DataStore, DataStore emite, el StateFlow cambia y la pantalla se recompone con el valor nuevo.
2. **`stateIn` y `WhileSubscribed(5_000)`.** El Flow del repositorio es frío y no tiene "valor
   actual". `stateIn` lo convierte en un `StateFlow` con `initialValue = UserPreferences()`. Con
   `WhileSubscribed(5_000)` la lectura sigue activa 5 s después de que nadie observa. Una
   rotación dura menos que eso, así que no reinicia la lectura; salir de la pantalla sí la detiene.
3. **`collectAsStateWithLifecycle` frente a `collectAsState`.** El primero deja de recolectar
   cuando la app pasa a segundo plano (`STOPPED`); el segundo sigue mientras el composable exista.
   Con el primero, `WhileSubscribed` puede apagar la lectura del disco cuando no se ve la pantalla.
4. **Por qué el Switch no guarda su propio estado.** Si lo guardara, habría **dos fuentes de
   verdad** (el Switch y DataStore) que pueden diferir. Ver el experimento.
5. **Fábrica de ViewModel.** `SettingsViewModel(repository)` tiene un parámetro, así que Android
   no sabe crearlo solo. `viewModelFactory { initializer { … } }` le enseña cómo hacerlo, sacando
   el repositorio del `AppContainer` de la Application.
6. **Accesibilidad.** Cada fila completa es el control (`Modifier.toggleable` con
   `role = Role.Switch`) y el `Switch` tiene `onCheckedChange = null`. TalkBack lee "Tema oscuro,
   interruptor, desactivado" como un solo elemento. `ListItem` mide ≥ 56 dp de alto,
   `IconButton` 48 dp y el engranaje tiene `contentDescription`.

## Guion para la clase (≈ 35 min)

1. Abrir la app, ir a Configuración y activar *Tema oscuro*. Cerrar la app desde Recientes,
   reabrirla y volver a Configuración: el interruptor sigue activado. Es la primera vez que algo
   **sobrevive al cierre**.
2. Rotar el dispositivo con la pantalla abierta: no se pierde ni se repite ningún cambio. El
   ViewModel sobrevive a la rotación y `viewModelScope` no se cancela.
3. Abrir `SettingsContent` en el panel de Preview. Mostrar que se dibuja sin ViewModel ni
   DataStore.
4. Ejecutar `SettingsContentTest`. Señalar la última línea de la primera prueba: después del clic
   el Switch **sigue apagado**, porque nadie le entregó un estado nuevo.
5. Experimento guiado (abajo).

## Experimento guiado: un Switch con estado propio

> Se hace en vivo en `FilaInterruptor` (`SettingsScreen.kt`) y se deshace al terminar.

Reemplazar el cuerpo de `FilaInterruptor` por:

```kotlin
var localActivado by remember { mutableStateOf(activado) }   // ← estado propio
ListItem(
    headlineContent = { Text(stringResource(titulo)) },
    trailingContent = { Switch(checked = localActivado, onCheckedChange = null) },
    modifier = modifier.toggleable(
        value = localActivado,
        role = Role.Switch,
        onValueChange = { localActivado = it; onCambio(it) }
    )
)
```

Pasos:

1. Activar *Tema oscuro*: **parece** funcionar, porque el Switch cambia al instante.
2. Cerrar la app por completo y reabrirla. Ir a Configuración.
3. **Inconsistencia:** el Switch aparece **apagado** aunque en DataStore dice `true`.

**Por qué ocurre.** `remember { mutableStateOf(activado) }` usa `activado` **solo la primera vez**
que se compone la fila. En ese momento el StateFlow todavía tiene su `initialValue`
(`UserPreferences()`, con `darkModeEnabled = false`) porque la lectura del disco no ha terminado.
Unos milisegundos después llega `true` desde DataStore, pero `remember` ya no vuelve a leer el
parámetro: el Switch queda con el `false` inicial. Ahora hay dos fuentes de verdad en desacuerdo:
DataStore dice "activado" y la pantalla dice "desactivado". (Con `remember(activado)` se
"arreglaría", pero entonces el estado local sobra: es exactamente el parámetro.)

**Conclusión:** el componente dibuja lo que recibe y avisa lo que el usuario quiere. La única
fuente de verdad es DataStore, expuesta por el StateFlow del ViewModel.

## Criterios de aceptación

- [x] La pantalla no conoce DataStore, Preferences ni PreferenceKeys:
  `git grep -n "import androidx.datastore\|PreferenceKeys\." -- "app/src/main/java/com/example/resumen_modulos1y2/ui/"`
  no devuelve resultados (las únicas menciones son comentarios).
- [x] Al rotar el dispositivo no se pierde ni se repite ningún cambio (ViewModel +
  `viewModelScope`; verificado en el emulador).
- [x] Prueba de UI con `createComposeRule` sobre `SettingsContent`: tocar el Switch invoca el
  callback con el valor correcto (`SettingsContentTest`, 3 pruebas).
