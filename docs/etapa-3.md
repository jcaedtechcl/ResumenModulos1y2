# Etapa 3 · Escribir con `edit`

**Objetivo:** modificar preferencias con transacciones suspendidas y atómicas.

**Qué se ve en la app:** todavía nada (la pantalla llega en la etapa 4). En clase se ejecutan las
pruebas del repositorio, que escriben en un archivo real.

## Qué se construyó

| Archivo | Rol |
|---|---|
| `data/preferences/UserPreferencesRepository.kt` | `suspend fun setDarkMode`, `setShowCompleted` y `setReminderOrder` con `dataStore.edit { }` |
| `test/.../UserPreferencesRepositoryTest.kt` | DataStore real en un archivo temporal (`TemporaryFolder`) + `runTest` |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | `kotlinx-coroutines-test` para `runTest` |

```kotlin
suspend fun setReminderOrder(order: ReminderOrder) {
    dataStore.edit { it[PreferenceKeys.REMINDER_ORDER] = order.name }
}
```

## Conceptos

1. **`suspend`.** Escribir en disco tarda. Una función `suspend` puede **pausarse** mientras espera
   sin bloquear el hilo. Por eso solo se llama desde una coroutine (`viewModelScope.launch` en la
   etapa 4, `runTest` en las pruebas). Si se llama desde un `onClick` normal, el compilador lo
   rechaza: *"Suspend function should be called only from a coroutine or another suspend function"*.
2. **`MutablePreferences`.** Dentro de `edit { }`, `it` es una **copia editable** del estado
   actual. Se modifica con `it[CLAVE] = valor` (o `it.remove(CLAVE)`) y, al terminar el bloque,
   DataStore guarda la copia completa.
3. **Atomicidad.** `edit` es una transacción: o se guarda todo el bloque o no se guarda nada. Las
   escrituras se ponen en una cola, así que dos `edit` simultáneos nunca se pisan. Como la copia
   ya trae las otras claves, cambiar una preferencia **no borra las demás**.
4. **El enum como texto.** DataStore no conoce `ReminderOrder`. Se guarda `order.name`
   (`"BY_PRIORITY"`) y el mapper de la etapa 2 lo convierte de vuelta, con un valor por defecto
   si el texto no se reconoce.

## Guion para la clase (≈ 25 min)

1. Escribir `setDarkMode` sin `suspend` y dejar que el compilador marque `edit`. Leer el error
   juntos y agregar `suspend`.
2. Abrir `UserPreferencesRepositoryTest`. Explicar `TemporaryFolder` (archivo nuevo en cada
   prueba) y `backgroundScope` (runTest lo cancela al terminar, así no quedan DataStores activos).
3. Ejecutar las cuatro pruebas. Luego, como experimento, cambiar `setShowCompleted` por
   `dataStore.edit { it.clear(); it[PreferenceKeys.SHOW_COMPLETED] = show }` y volver a ejecutar:
   falla `cambiarUnaPreferencia_noAlteraLasOtras`. Conversar por qué y deshacer el cambio.
4. Pregunta de cierre: *¿por qué la tercera prueba (mismo valor dos veces) es importante para
   una pantalla que se puede recomponer o rotar?* (Una escritura repetida debe ser inofensiva:
   el resultado no depende de cuántas veces se aplicó.)

## Pruebas

| Prueba | Qué demuestra |
|---|---|
| `escribirYLeer` | lo escrito con `edit` se lee con `preferences.first()` ya convertido a `UserPreferences` |
| `cambiarUnaPreferencia_noAlteraLasOtras` | `edit` parte de una copia con todas las claves |
| `escribirElMismoValorDosVeces_dejaElMismoResultado` | escribir es idempotente |
| `sinEscrituras_entregaLosValoresPorDefecto` | un archivo vacío produce `UserPreferences()` |

Comando: `./gradlew :app:testDebugUnitTest --tests "*UserPreferencesRepositoryTest"` → 4 tests, 0 failures.
