package com.example.resumen_modulos1y2.ui.flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Flow "frío": el bloque flow { } no hace nada hasta que alguien lo recolecta,
 * y cada recolección lo ejecuta de nuevo desde el principio.
 */
fun temporizadorEnfoque(segundos: Int = 5): Flow<Int> = flow {
    for (restante in segundos downTo 0) {
        emit(restante)
        if (restante > 0) delay(1_000)
    }
}

private val catalogoTareas = listOf(
    "Comprar pan", "Comprar fruta", "Pagar la luz", "Pagar el arriendo",
    "Estudiar Compose", "Estudiar Kotlin", "Llamar al médico", "Lavar la ropa",
    "Leer un capítulo", "Regar las plantas"
)

class FlowViewModel : ViewModel() {

    // 2. StateFlow: siempre tiene un valor y todos los que lo observan ven el mismo

    private val _completadasHoy = MutableStateFlow(0)
    val completadasHoy: StateFlow<Int> = _completadasHoy.asStateFlow()

    fun completarUna() = _completadasHoy.update { it + 1 }
    fun reiniciar() {
        _completadasHoy.value = 0
    }

    // 3. Operadores: de un StateFlow de texto a un StateFlow de resultados

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _pulsaciones = MutableStateFlow(0)
    val pulsaciones: StateFlow<Int> = _pulsaciones.asStateFlow()

    private val _filtrados = MutableStateFlow(0)
    val filtrados: StateFlow<Int> = _filtrados.asStateFlow()

    fun buscar(texto: String) {
        _busqueda.value = texto
        _pulsaciones.update { it + 1 }
    }

    @OptIn(FlowPreview::class)
    val resultados: StateFlow<List<String>> = _busqueda
        // espera 400 ms sin cambios antes de seguir: no filtra en cada tecla
        .debounce(400)
        .map { texto ->
            _filtrados.update { it + 1 }
            catalogoTareas.filter { it.contains(texto.trim(), ignoreCase = true) }
        }
        // stateIn convierte el Flow en StateFlow. WhileSubscribed(5_000) lo mantiene activo
        // 5 s después del último observador (suficiente para sobrevivir a una rotación).
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), catalogoTareas)

    // 4. Ciclo de vida: un reloj frío que deja registro de cuándo se recolecta

    private val _eventos = MutableStateFlow<List<String>>(emptyList())
    val eventos: StateFlow<List<String>> = _eventos.asStateFlow()

    val reloj: Flow<Int> = flow {
        var segundos = 0
        while (true) {
            emit(segundos++)
            delay(1_000)
        }
    }
        .onStart { registrar("▶ Comenzó la recolección") }
        .onCompletion { registrar("■ Se detuvo la recolección") }

    private fun registrar(evento: String) {
        val hora = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _eventos.update { (listOf("$hora  $evento") + it).take(8) }
    }

    fun limpiarEventos() {
        _eventos.value = emptyList()
    }
}
