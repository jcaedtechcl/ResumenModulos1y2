package com.example.resumen_modulos1y2.iss.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Las lecturas devuelven Flow: Room vuelve a emitir cada vez que la tabla cambia. Así la pantalla
 * se entera sola de lo que guardan la consulta en primer plano y el worker de la etapa 5.
 */
@Dao
interface IssPositionDao {

    /** La medición más reciente, o null si la tabla está vacía (vacío no es error). */
    @Query("SELECT * FROM iss_positions ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<IssPositionEntity?>

    @Query("SELECT * FROM iss_positions ORDER BY timestamp DESC LIMIT :limit")
    fun observeHistory(limit: Int = 20): Flow<List<IssPositionEntity>>

    /** Inserta o reemplaza: la misma medición (mismo timestamp) nunca queda dos veces. */
    @Upsert
    suspend fun save(position: IssPositionEntity)

    /** Borra lo anterior a `timestamp` (segundos Unix); la app conserva solo las últimas 24 h. */
    @Query("DELETE FROM iss_positions WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
