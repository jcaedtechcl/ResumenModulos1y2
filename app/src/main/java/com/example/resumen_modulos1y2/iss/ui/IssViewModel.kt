package com.example.resumen_modulos1y2.iss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.resumen_modulos1y2.appContainer
import com.example.resumen_modulos1y2.iss.data.IssRepository
import com.example.resumen_modulos1y2.iss.data.remote.RemoteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Tiempo mínimo entre consultas exitosas. La API admite ≈1 por segundo; aquí se pide 10 veces menos. */
const val INTERVALO_MINIMO_MS = 10_000L

/**
 * ETAPA 2: el ViewModel recibe RemoteResult y lo convierte en IssUiState. Ya no hay try/catch:
 * las excepciones se quedaron en la capa de datos (safeApiCall).
 *
 * `reloj` se recibe como parámetro para que la prueba pueda "adelantar" el tiempo.
 */
class IssViewModel(
    private val repository: IssRepository,
    private val reloj: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow<IssUiState>(IssUiState.Loading)
    val uiState: StateFlow<IssUiState> = _uiState.asStateFlow()

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
        _uiState.value = IssUiState.Loading
        viewModelScope.launch {
            val resultado = repository.fetchPosition()
            _uiState.value = when (resultado) {
                is RemoteResult.Success -> {
                    ultimoExito = reloj()
                    IssUiState.Content(resultado.data)
                }
                else -> resultado.aError()
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
