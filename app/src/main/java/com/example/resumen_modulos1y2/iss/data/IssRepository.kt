package com.example.resumen_modulos1y2.iss.data

import com.example.resumen_modulos1y2.iss.data.local.IssPositionDao
import com.example.resumen_modulos1y2.iss.data.remote.IssApiService
import com.example.resumen_modulos1y2.iss.data.remote.RemoteResult
import com.example.resumen_modulos1y2.iss.data.remote.map
import com.example.resumen_modulos1y2.iss.data.remote.safeApiCall
import com.example.resumen_modulos1y2.iss.domain.IssPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Lo ÚNICO que necesita el worker de la etapa 5: pedir una actualización. Una interfaz pequeña
 * permite probar el worker con un repositorio falso de una línea.
 */
fun interface IssRefresher {
    suspend fun refresh(): RemoteResult<Unit>
}

/** Cuánto historial se conserva en la base. */
private const val RETENCION_SEGUNDOS = 24 * 60 * 60L

/**
 * Local-first: Room es la ÚNICA fuente de verdad.
 *
 *            refresh()                       latest / history (Flow)
 *   API ──────────────► Room (iss_positions) ─────────────────────────► ViewModel ─► pantalla
 *
 * - `latest` y `history` vienen SIEMPRE de Room. Con o sin red, la pantalla muestra lo que hay
 *   en la base.
 * - `refresh()` consulta la API y GUARDA el resultado. No devuelve la posición: el resultado le
 *   llega a la pantalla porque Room emite la fila nueva. `RemoteResult<Unit>` informa solo si
 *   la actualización funcionó.
 * - Un refresh fallido no toca la base: lo último conocido sigue ahí.
 *
 * Hay UNA instancia (AppContainer) compartida por la pantalla y, desde la etapa 5, por el worker.
 *
 * `reloj` (milisegundos) se recibe como parámetro para poder fijar la hora en una prueba.
 */
class IssRepository(
    private val api: IssApiService,
    private val dao: IssPositionDao,
    private val reloj: () -> Long = System::currentTimeMillis
) : IssRefresher {

    val latest: Flow<IssPosition?> = dao.observeLatest().map { it?.toDomain() }

    val history: Flow<List<IssPosition>> = dao.observeHistory().map { filas -> filas.map { it.toDomain() } }

    /**
     * Mutex: un candado para coroutines. Solo una coroutine a la vez ejecuta lo que está dentro de
     * `withLock`; las demás ESPERAN su turno sin bloquear el hilo, a diferencia de `synchronized`.
     * Así, aunque el bucle de la pantalla, la vuelta de la red y un toque del usuario pidan
     * refresh al mismo tiempo, nunca hay dos solicitudes en curso.
     */
    private val mutex = Mutex()

    private val _isRefreshing = MutableStateFlow(false)

    /** true mientras hay una solicitud en curso; la pantalla muestra un indicador. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    override suspend fun refresh(): RemoteResult<Unit> = mutex.withLock {
        _isRefreshing.value = true
        try {
            val resultado = safeApiCall { api.getPosition() }
            if (resultado is RemoteResult.Success) {
                dao.save(resultado.data.toEntity())
                dao.deleteOlderThan(reloj() / 1000 - RETENCION_SEGUNDOS)
            }
            resultado.map { }
        } finally {
            // finally: el indicador se apaga incluso si la coroutine se cancela a mitad de camino
            _isRefreshing.value = false
        }
    }
}
