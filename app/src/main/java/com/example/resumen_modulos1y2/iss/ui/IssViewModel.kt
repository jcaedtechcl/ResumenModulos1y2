package com.example.resumen_modulos1y2.iss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.resumen_modulos1y2.appContainer
import com.example.resumen_modulos1y2.iss.data.IssRepository
import com.example.resumen_modulos1y2.iss.domain.IssPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ETAPA 1: estado mínimo de la pantalla. En la etapa 2 se reemplaza por IssUiState (sealed). */
data class ConsultaIss(
    val cargando: Boolean = false,
    val posicion: IssPosition? = null,
    val error: String? = null
)

/**
 * ETAPA 1: el ViewModel pide la posición al repositorio y la deja en un StateFlow.
 *
 * El `catch (e: Exception)` es ingenuo a propósito: muestra el mensaje técnico de la excepción
 * ("Unable to resolve host…", "CLEARTEXT communication…"). Sirve para el experimento guiado y
 * motiva la etapa 2, donde cada error se clasifica en la capa de datos.
 */
class IssViewModel(private val repository: IssRepository) : ViewModel() {

    private val _estado = MutableStateFlow(ConsultaIss())
    val estado: StateFlow<ConsultaIss> = _estado.asStateFlow()

    fun consultar() {
        if (_estado.value.cargando) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val posicion = repository.fetchPosition()
                _estado.update { it.copy(cargando = false, posicion = posicion) }
            } catch (e: Exception) {
                _estado.update { it.copy(cargando = false, error = e.message ?: e.javaClass.simpleName) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { IssViewModel(appContainer().issRepository) }
        }
    }
}
