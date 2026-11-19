package com.example.resumen_modulos1y2.iss.ui

import androidx.annotation.StringRes
import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.iss.data.remote.RemoteResult
import com.example.resumen_modulos1y2.iss.domain.IssPosition

/**
 * Lo único que la pantalla necesita saber. Es `sealed`: la pantalla resuelve un `when` con tres
 * casos y nunca recibe una excepción ni un código HTTP.
 */
sealed interface IssUiState {
    data object Loading : IssUiState
    data class Content(val position: IssPosition) : IssUiState

    /** `mensaje` es un recurso de strings.xml: la pantalla lo traduce con stringResource(). */
    data class Error(@StringRes val mensaje: Int, val puedeReintentar: Boolean) : IssUiState
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
