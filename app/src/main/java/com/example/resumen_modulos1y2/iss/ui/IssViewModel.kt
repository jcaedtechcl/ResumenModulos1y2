package com.example.resumen_modulos1y2.iss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.resumen_modulos1y2.appContainer
import com.example.resumen_modulos1y2.iss.data.IssRepository
import com.example.resumen_modulos1y2.iss.data.remote.RemoteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Tiempo mínimo entre consultas exitosas. La API admite ≈1 por segundo; aquí se pide 10 veces menos. */
const val INTERVALO_MINIMO_MS = 10_000L

/**
 * ETAPA 3: el ViewModel ya no guarda la posición. La pantalla se arma combinando:
 * - lo que hay en Room (`repository.latest` y `repository.history`), y
 * - el error de la última consulta, si hubo.
 *
 * `consultar()` solo PIDE una actualización. Si funciona, Room emite la fila nueva y la pantalla
 * cambia sola; el ViewModel nunca recibe la posición desde la red.
 *
 * `reloj` se recibe como parámetro para que la prueba pueda "adelantar" el tiempo.
 */
class IssViewModel(
    private val repository: IssRepository,
    private val reloj: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val ultimoError = MutableStateFlow<IssUiState.Error?>(null)

    val uiState: StateFlow<IssUiState> =
        combine(repository.latest, repository.history, ultimoError, ::construirUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = IssUiState.Loading
            )

    private var consultando = false
    private var ultimoExito: Long? = null

    init {
        consultar()
    }

    /**
     * Control de frecuencia: durante 10 s después de una consulta exitosa los toques se ignoran.
     * También se ignoran mientras hay una consulta en curso. Así, aunque el usuario toque el
     * botón diez veces seguidas, sale UNA solicitud y nunca se llega al 429 de la API.
     */
    fun consultar() {
        if (consultando) return
        val ultimo = ultimoExito
        if (ultimo != null && reloj() - ultimo < INTERVALO_MINIMO_MS) return

        consultando = true
        viewModelScope.launch {
            when (val resultado = repository.refresh()) {
                is RemoteResult.Success -> {
                    ultimoExito = reloj()
                    ultimoError.value = null
                }
                else -> ultimoError.value = resultado.aError()
            }
            consultando = false
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { IssViewModel(appContainer().issRepository) }
        }
    }
}
