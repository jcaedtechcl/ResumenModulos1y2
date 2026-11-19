# Etapa 1 · Modelo, claves tipadas y una única instancia

**Objetivo:** distinguir el modelo propio de la app de los pares clave-valor que guarda DataStore.

**Qué se ve en la app:** nada cambia todavía. Esta etapa prepara la infraestructura; la clase se
centra en leer el código y el diagrama.

## Qué se construyó

| Archivo | Rol |
|---|---|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | dependencia `androidx.datastore:datastore-preferences` |
| `data/preferences/ReminderOrder.kt` | `enum class ReminderOrder { BY_DATE, BY_PRIORITY }` |
| `data/preferences/UserPreferences.kt` | modelo propio con valores por defecto |
| `data/preferences/PreferenceKeys.kt` | `internal object` con las tres claves tipadas |
| `data/preferences/UserPreferencesDataStore.kt` | el delegado `preferencesDataStore`, declarado **una vez** |
| `di/AppContainer.kt` | contenedor de dependencias; obtiene el DataStore con `applicationContext` |
| `RecordatoriosApp.kt` + `AndroidManifest.xml` | `Application` que crea el contenedor (`android:name=".RecordatoriosApp"`) |

```
              lo que ve la app                        lo que hay en disco
┌───────────────────────────────────────┐    ┌──────────────────────────────────┐
│ UserPreferences (nuestro)             │    │ Preferences (librería)           │
│   darkModeEnabled: Boolean            │◄──►│   "dark_mode"      → true        │
│   showCompleted:   Boolean            │    │   "show_completed" → false       │
│   reminderOrder:   ReminderOrder      │    │   "reminder_order" → "BY_DATE"   │
└───────────────────────────────────────┘    └──────────────────────────────────┘
                                       PreferenceKeys
                     (solo data/preferences sabe traducir de un lado al otro)
```

## Conceptos

1. **`Preferences` frente a `UserPreferences`.** `Preferences` es de la librería: un mapa de
   claves a valores primitivos, sin nada de nuestro dominio. `UserPreferences` es nuestra: tiene
   nombres con sentido, un enum en vez de un String y valores por defecto. La UI solo conocerá
   `UserPreferences`.
2. **Claves tipadas.** `booleanPreferencesKey("dark_mode")` es una `Preferences.Key<Boolean>`:
   leer con ella devuelve `Boolean?`, y el compilador impide guardar un String en esa clave.
3. **El nombre en disco no se cambia.** El texto `"dark_mode"` identifica el dato en el archivo. Si
   se renombra, la app no encuentra lo que ya estaba guardado y vuelve a los valores por defecto
   sin avisar. Renombrar la constante de Kotlin (`DARK_MODE`) sí es seguro.
4. **Una única instancia.** El delegado `by preferencesDataStore(...)` en el nivel superior crea
   el DataStore la primera vez y luego entrega siempre el mismo objeto.
5. **`applicationContext`.** El DataStore vive tanto como el proceso. Si el contenedor guardara
   una Activity, al rotar quedaría referenciando una Activity destruida (fuga de memoria).

## Guion para la clase (≈ 20 min)

1. Abrir `UserPreferences.kt` y preguntar: *¿qué valores debería ver la app la primera vez que
   se instala?* Esos son los valores por defecto.
2. Abrir `PreferenceKeys.kt`. Mostrar que `DARK_MODE` es `Preferences.Key<Boolean>` (Ctrl+Q en
   Android Studio) y leer en voz alta la advertencia sobre los nombres en disco.
3. Abrir `UserPreferencesDataStore.kt` y luego `AppContainer.kt`. Recorrer el camino:
   `RecordatoriosApp.onCreate` → `AppContainer(this)` → `applicationContext.userPreferencesDataStore`.
4. Hacer el experimento guiado de más abajo.

## Experimento guiado: dos DataStores para el mismo archivo

> Este experimento se hace **en vivo** y se deshace al terminar. No queda en el repositorio.

1. En cualquier archivo nuevo (por ejemplo `data/preferences/Duplicado.kt`) declarar un segundo
   delegado con el **mismo nombre**:
   ```kotlin
   val Context.otroDataStore by preferencesDataStore(name = "user_preferences")
   ```
2. En `AppContainer`, leer de los dos (desde una coroutine, por ejemplo
   `MainScope().launch { ... }` dentro de un bloque temporal):
   ```kotlin
   appContext.userPreferencesDataStore.data.first()
   appContext.otroDataStore.data.first()
   ```
3. Ejecutar. La segunda lectura falla con:
   ```
   java.lang.IllegalStateException: There are multiple DataStores active for the same file:
   /data/user/0/com.example.resumen_modulos1y2/files/datastore/user_preferences.preferences_pb.
   You should either maintain your DataStore as a singleton or confirm that there is no two
   DataStore's active on the same file (by confirming that the scope is cancelled).
   ```

**Por qué existe la regla.** Cada instancia de DataStore guarda en memoria su propia copia de los
datos y coordina sus escrituras con una cola interna. Dos instancias sobre el mismo archivo no se
ven entre sí: una podría escribir encima de la otra con datos viejos y el archivo quedaría
inconsistente. DataStore prefiere fallar en voz alta a perder datos en silencio. La solución es
la de esta etapa: **una sola declaración** y pedirla siempre al `AppContainer`.

Pregunta para los estudiantes: *¿qué pasaría si declaráramos el delegado dentro de una clase en
vez del nivel superior y creáramos dos objetos de esa clase?* (Respuesta: cada objeto tendría su
propio delegado, así que habría dos instancias y el mismo error.)

## Criterios de aceptación

- [x] Ninguna clase fuera de `data/preferences` usa `PreferenceKeys`. Verificación:
  `git grep -l PreferenceKeys -- app/src/main` solo lista archivos de `data/preferences/`.
- [x] Los nombres de las claves en disco tienen un comentario que advierte que no deben cambiar
  (`PreferenceKeys.kt`).
- [x] Existe una sola declaración `by preferencesDataStore` (`git grep preferencesDataStore`).
