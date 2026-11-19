package com.example.resumen_modulos1y2.iss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.resumen_modulos1y2.appContainer
import com.example.resumen_modulos1y2.iss.data.ConnectivityObserver
import com.example.resumen_modulos1y2.iss.data.IssRepository
import com.example.resumen_modulos1y2.iss.data.remote.RemoteResult
import com.example.resumen_modulos1y2.iss.domain.EstadoSincronizacion
import com.example.resumen_modulos1y2.iss.work.BackgroundSync
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Tiempo mínimo entre consultas exitosas. La API admite ≈1 por segundo; aquí se pide 10 veces menos. */
const val INTERVALO_MINIMO_MS = 10_000L

/**
 * El ViewModel no guarda la posición. La pantalla se arma combinando:
 * - lo que hay en Room (`repository.latest` y `repository.history`);
 * - el error de la última consulta, si hubo;
 * - si hay red (`connectivity.isOnline`) y si hay una solicitud en curso (`repository.isRefreshing`).
 *
 * `consultar()` solo PIDE una actualización. Si funciona, Room emite la fila nueva y la pantalla
 * cambia sola.
 *
 * `reloj` se recibe como parámetro para que la prueba pueda "adelantar" el tiempo.
 */
class IssViewModel(
    private val repository: IssRepository,
    connectivity: ConnectivityObserver,
    private val backgroundSync: BackgroundSync,
    private val reloj: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val ultimoError = MutableStateFlow<IssUiState.Error?>(null)

    // Un solo registro del callback de red, compartido por el estado y el bucle de actualización
    private val enLinea = connectivity.isOnline
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val uiState: StateFlow<IssUiState> =
        combine(repository.latest, repository.history, ultimoError, enLinea, repository.isRefreshing, ::construirUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = IssUiState.Loading
            )

    /**
     * Estado de los trabajos de WorkManager, aparte de IssUiState: describe lo que pasa FUERA de
     * esta pantalla (el sistema puede ejecutarlos con la app cerrada), no lo que se está viendo.
     */
    val estadoSegundoPlano: StateFlow<EstadoSincronizacion> = backgroundSync.estado
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoSincronizacion())

    private var ultimoExito: Long? = null

    /** Encola un trabajo único inmediato. Con KEEP, cinco toques siguen siendo un solo trabajo. */
    fun actualizarEnSegundoPlano() {
        backgroundSync.actualizarAhora()
    }

    /** Toque en "Consultar": lanza la actualización sin esperar el resultado. */
    fun consultar() {
        viewModelScope.launch { actualizarSiCorresponde() }
    }

    /**
     * Control de frecuencia: durante 10 s después de una consulta exitosa los pedidos se ignoran,
     * y también mientras hay una solicitud en curso. Aunque el usuario toque el botón diez veces
     * seguidas, sale UNA solicitud.
     *
     * Es `suspend` y TERMINA cuando termina la consulta. Así el bucle de abajo puede esperar la
     * respuesta antes de contar los 10 s siguientes.
     */
    private suspend fun actualizarSiCorresponde() {
        if (repository.isRefreshing.value) return
        val ultimo = ultimoExito
        if (ultimo != null && reloj() - ultimo < INTERVALO_MINIMO_MS) return

        when (val resultado = repository.refresh()) {
            is RemoteResult.Success -> {
                ultimoExito = reloj()
                ultimoError.value = null
            }
            else -> ultimoError.value = resultado.aError()
        }
    }

    /**
     * Actualización automática MIENTRAS LA PANTALLA ESTÁ VISIBLE. La pantalla la llama dentro de
     * `repeatOnLifecycle(STARTED)`: al pasar a segundo plano o salir, esta función se cancela y el
     * bucle se detiene. No queda ninguna solicitud periódica corriendo sin que nadie mire.
     *
     * `collectLatest` sobre "¿hay red?":
     * - con red: consulta ahora y luego cada 10 s;
     * - sin red: no intenta nada (sería un fallo seguro) y espera;
     * - al volver la red (false → true): el bloque se reinicia y consulta DE INMEDIATO. Ese es
     *   el refresh automático al reconectar.
     * Cada cambio cancela el bucle anterior, así que nunca hay dos bucles a la vez.
     */
    suspend fun actualizarMientrasVisible() {
        enLinea.collectLatest { online ->
            if (!online) return@collectLatest
            while (true) {
                // Se ESPERA la respuesta y recién entonces se cuentan 10 s. Si se contara desde
                // que sale la solicitud, una respuesta lenta haría que la vuelta siguiente cayera
                // dentro del control de frecuencia y se saltara.
                actualizarSiCorresponde()
                delay(INTERVALO_MINIMO_MS)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                IssViewModel(container.issRepository, container.connectivityObserver, container.issWorkScheduler)
            }
        }
    }
}
