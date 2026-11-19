package com.example.resumen_modulos1y2.iss.data

import com.example.resumen_modulos1y2.iss.data.local.IssPositionDao
import com.example.resumen_modulos1y2.iss.data.remote.IssApiService
import com.example.resumen_modulos1y2.iss.data.remote.RemoteResult
import com.example.resumen_modulos1y2.iss.data.remote.map
import com.example.resumen_modulos1y2.iss.data.remote.safeApiCall
import com.example.resumen_modulos1y2.iss.domain.IssPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Cuánto historial se conserva en la base. */
private const val RETENCION_SEGUNDOS = 24 * 60 * 60L

/**
 * ETAPA 3: local-first. Room es la ÚNICA fuente de verdad.
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
 * `reloj` (milisegundos) se recibe como parámetro para poder fijar la hora en una prueba.
 */
class IssRepository(
    private val api: IssApiService,
    private val dao: IssPositionDao,
    private val reloj: () -> Long = System::currentTimeMillis
) {

    val latest: Flow<IssPosition?> = dao.observeLatest().map { it?.toDomain() }

    val history: Flow<List<IssPosition>> = dao.observeHistory().map { filas -> filas.map { it.toDomain() } }

    suspend fun refresh(): RemoteResult<Unit> {
        val resultado = safeApiCall { api.getPosition() }
        if (resultado is RemoteResult.Success) {
            dao.save(resultado.data.toEntity())
            dao.deleteOlderThan(reloj() / 1000 - RETENCION_SEGUNDOS)
        }
        return resultado.map { }
    }
}
