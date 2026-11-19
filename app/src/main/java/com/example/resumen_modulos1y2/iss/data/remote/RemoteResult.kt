package com.example.resumen_modulos1y2.iss.data.remote

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Todo lo que puede salir de una llamada de red, como VALORES y no como excepciones.
 *
 * Al ser `sealed`, el compilador conoce todas las variantes: un `when` sobre RemoteResult sin
 * `else` obliga a decidir qué hacer en cada caso. Si mañana se agrega una variante, el código
 * que no la trate deja de compilar.
 *
 * `out T`: un RemoteResult<Nothing> (los errores) sirve donde se espera un RemoteResult<Lo que sea>.
 */
sealed interface RemoteResult<out T> {

    data class Success<T>(val data: T) : RemoteResult<T>

    /** El servidor respondió con un código que no es 2xx (distinto de 429). 4xx = la solicitud
     *  está mal y repetirla no sirve; 5xx = el servidor falló y más tarde puede funcionar. */
    data class HttpError(val code: Int) : RemoteResult<Nothing> {
        val esErrorDelServidor: Boolean get() = code in 500..599
    }

    /** HTTP 429 Too Many Requests: se superó el límite de la API. Hay que esperar, no insistir. */
    data object RateLimited : RemoteResult<Nothing>

    /** Hubo conexión, pero la respuesta no llegó a tiempo (timeouts de AppContainer). */
    data object Timeout : RemoteResult<Nothing>

    /** No se pudo llegar al servidor: sin red, DNS que no resuelve, conexión rechazada… */
    data object NoConnection : RemoteResult<Nothing>

    /** Llegó una respuesta, pero no cumple el contrato del DTO (JSON roto o tipos distintos). */
    data object InvalidData : RemoteResult<Nothing>
}

/** Transforma el dato de un Success (por ejemplo DTO → dominio) y deja los errores intactos. */
inline fun <T, R> RemoteResult<T>.map(transform: (T) -> R): RemoteResult<R> = when (this) {
    is RemoteResult.Success -> RemoteResult.Success(transform(data))
    is RemoteResult.HttpError -> this
    RemoteResult.RateLimited -> RemoteResult.RateLimited
    RemoteResult.Timeout -> RemoteResult.Timeout
    RemoteResult.NoConnection -> RemoteResult.NoConnection
    RemoteResult.InvalidData -> RemoteResult.InvalidData
}

private const val HTTP_TOO_MANY_REQUESTS = 429

/**
 * El único lugar donde se atrapan las excepciones de red. Hacia afuera solo salen RemoteResult:
 * ni el ViewModel ni la pantalla necesitan saber qué es una SocketTimeoutException.
 *
 * El ORDEN de los catch importa: SocketTimeoutException, JsonEncodingException y EOFException
 * también son IOException, así que van antes que el caso general.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): RemoteResult<T> = try {
    val response = call()
    val body = response.body()
    when {
        response.code() == HTTP_TOO_MANY_REQUESTS -> RemoteResult.RateLimited
        !response.isSuccessful -> RemoteResult.HttpError(response.code())
        body == null -> RemoteResult.InvalidData // 200 sin cuerpo: no cumple el contrato
        else -> RemoteResult.Success(body)
    }
} catch (e: CancellationException) {
    // Cancelar una coroutine (por ejemplo, al salir de la pantalla) NO es un error de red: se
    // relanza para que la cancelación siga su curso.
    throw e
} catch (_: SocketTimeoutException) {
    RemoteResult.Timeout
} catch (_: JsonDataException) {
    // JSON bien formado, pero con un campo faltante o de otro tipo
    RemoteResult.InvalidData
} catch (_: JsonEncodingException) {
    // No es JSON (por ejemplo, una página HTML de error)
    RemoteResult.InvalidData
} catch (_: EOFException) {
    // El cuerpo terminó antes de lo esperado (JSON cortado o vacío)
    RemoteResult.InvalidData
} catch (_: IOException) {
    RemoteResult.NoConnection
}
