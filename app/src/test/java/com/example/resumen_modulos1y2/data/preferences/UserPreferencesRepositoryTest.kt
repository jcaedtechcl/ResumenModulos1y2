package com.example.resumen_modulos1y2.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Pruebas con un DataStore REAL que escribe en un archivo temporal: se prueba el
 * comportamiento de verdad (escritura, lectura y atomicidad), no una imitación.
 */
class UserPreferencesRepositoryTest {

    // Crea una carpeta nueva antes de cada prueba y la borra después: cada prueba parte de cero
    @get:Rule
    val tmp = TemporaryFolder()

    /**
     * Cada prueba crea su propio DataStore sobre `backgroundScope`, que runTest cancela al
     * terminar. Así no quedan dos DataStores activos sobre el mismo archivo (etapa 1).
     */
    private fun TestScope.crearRepositorio() = UserPreferencesRepository(
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tmp.newFile("test.preferences_pb") }
        )
    )

    @Test
    fun escribirYLeer() = runTest {
        val repositorio = crearRepositorio()

        repositorio.setDarkMode(true)
        repositorio.setShowCompleted(false)
        repositorio.setReminderOrder(ReminderOrder.BY_PRIORITY)

        // first() toma el valor actual del flujo y deja de recolectar
        assertEquals(
            UserPreferences(darkModeEnabled = true, showCompleted = false, ReminderOrder.BY_PRIORITY),
            repositorio.preferences.first()
        )
    }

    @Test
    fun cambiarUnaPreferencia_noAlteraLasOtras() = runTest {
        val repositorio = crearRepositorio()
        repositorio.setDarkMode(true)
        repositorio.setReminderOrder(ReminderOrder.BY_PRIORITY)

        repositorio.setShowCompleted(false)

        val resultado = repositorio.preferences.first()
        assertEquals(true, resultado.darkModeEnabled)
        assertEquals(ReminderOrder.BY_PRIORITY, resultado.reminderOrder)
        assertEquals(false, resultado.showCompleted)
    }

    @Test
    fun escribirElMismoValorDosVeces_dejaElMismoResultado() = runTest {
        val repositorio = crearRepositorio()

        repositorio.setReminderOrder(ReminderOrder.BY_PRIORITY)
        val despuesDeLaPrimera = repositorio.preferences.first()
        repositorio.setReminderOrder(ReminderOrder.BY_PRIORITY)

        assertEquals(despuesDeLaPrimera, repositorio.preferences.first())
    }

    @Test
    fun sinEscrituras_entregaLosValoresPorDefecto() = runTest {
        assertEquals(UserPreferences(), crearRepositorio().preferences.first())
    }
}
