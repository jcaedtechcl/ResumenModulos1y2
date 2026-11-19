package com.example.resumen_modulos1y2.iss.ui

import androidx.annotation.StringRes
import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.iss.data.remote.RemoteResult
import com.example.resumen_modulos1y2.iss.domain.IssPosition
import java.time.Duration
import java.time.Instant

/**
 * Lo único que la pantalla necesita saber. Es `sealed`, así que la pantalla resuelve un `when`
 * y nunca recibe una excepción ni un código HTTP.
 *
 * Desde la etapa 3, "vacío" y "error" son estados distintos:
 * - Empty: la base no tiene posiciones y nada falló (por ejemplo, la primera consulta va en camino).
 * - Error: la base no tiene posiciones Y la última consulta falló. No hay nada que mostrar.
 * - Content: hay al menos una posición guardada. Si además la última consulta falló, el error
 *   viaja en `error` y se muestra JUNTO a los datos, sin reemplazarlos.
 */
sealed interface IssUiState {
    /** Todavía no llega la primera lectura de Room (dura milisegundos). */
    data object Loading : IssUiState

    data object Empty : IssUiState

    data class Content(
        val position: IssPosition,
        val history: List<IssPosition>,
        val error: Error? = null
    ) : IssUiState

    /** `mensaje` es un recurso de strings.xml: la pantalla lo traduce con stringResource(). */
    data class Error(@StringRes val mensaje: Int, val puedeReintentar: Boolean) : IssUiState
}

/** Combina lo que hay en Room con el resultado de la última consulta. Función pura. */
fun construirUiState(latest: IssPosition?, history: List<IssPosition>, ultimoError: IssUiState.Error?): IssUiState =
    when {
        latest != null -> IssUiState.Content(latest, history, ultimoError)
        ultimoError != null -> ultimoError
        else -> IssUiState.Empty
    }

/**
 * Traduce un fallo técnico a algo que el usuario entiende y decide si tiene sentido reintentar.
 * `when` exhaustivo, sin `else`: si RemoteResult gana una variante, esto deja de compilar.
 */
fun RemoteResult<*>.aError(): IssUiState.Error = when (this) {
    is RemoteResult.Success -> error("Un Success no es un error")
    RemoteResult.NoConnection -> IssUiState.Error(R.string.iss_error_sin_conexion, puedeReintentar = true)
    RemoteResult.Timeout -> IssUiState.Error(R.string.iss_error_timeout, puedeReintentar = true)
    RemoteResult.RateLimited -> IssUiState.Error(R.string.iss_error_limite, puedeReintentar = true)
    RemoteResult.InvalidData -> IssUiState.Error(R.string.iss_error_datos, puedeReintentar = false)
    is RemoteResult.HttpError ->
        if (esErrorDelServidor) IssUiState.Error(R.string.iss_error_servidor, puedeReintentar = true)
        else IssUiState.Error(R.string.iss_error_solicitud, puedeReintentar = false)
}

/** Cuánto tiempo pasó desde una medición, ya redondeado para mostrarlo. */
sealed interface Antiguedad {
    data object RecienMedida : Antiguedad
    data class Minutos(val cantidad: Long) : Antiguedad
    data class Horas(val cantidad: Long) : Antiguedad
    data class Dias(val cantidad: Long) : Antiguedad
}

/**
 * Función PURA: mismo `medida` y mismo `ahora`, mismo resultado. No lee el reloj ni recursos,
 * así que se prueba con un test unitario simple. La pantalla convierte el resultado en texto
 * ("hace 3 min") con plurals de strings.xml.
 */
fun antiguedad(medida: Instant, ahora: Instant): Antiguedad {
    // Si el reloj del teléfono está atrasado, la medición puede parecer "del futuro"
    val transcurrido = Duration.between(medida, ahora).coerceAtLeast(Duration.ZERO)
    return when {
        transcurrido.toMinutes() < 1 -> Antiguedad.RecienMedida
        transcurrido.toHours() < 1 -> Antiguedad.Minutos(transcurrido.toMinutes())
        transcurrido.toDays() < 1 -> Antiguedad.Horas(transcurrido.toHours())
        else -> Antiguedad.Dias(transcurrido.toDays())
    }
}
