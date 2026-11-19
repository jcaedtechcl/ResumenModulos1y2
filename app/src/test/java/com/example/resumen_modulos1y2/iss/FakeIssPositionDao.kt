package com.example.resumen_modulos1y2.iss

import com.example.resumen_modulos1y2.iss.data.local.IssPositionDao
import com.example.resumen_modulos1y2.iss.data.local.IssPositionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * DAO falso para pruebas en la JVM: IssPositionDao es una interfaz, así que basta con un mapa en
 * memoria dentro de un StateFlow. Se comporta como Room: cada cambio vuelve a emitir.
 * (Las consultas SQL reales se prueban en androidTest con Room en memoria.)
 */
class FakeIssPositionDao : IssPositionDao {

    val filas = MutableStateFlow<Map<Long, IssPositionEntity>>(emptyMap())

    override fun observeLatest(): Flow<IssPositionEntity?> = filas.map { it.values.maxByOrNull(IssPositionEntity::timestamp) }

    override fun observeHistory(limit: Int): Flow<List<IssPositionEntity>> =
        filas.map { it.values.sortedByDescending(IssPositionEntity::timestamp).take(limit) }

    override suspend fun save(position: IssPositionEntity) {
        filas.value += position.timestamp to position
    }

    override suspend fun deleteOlderThan(timestamp: Long) {
        filas.value = filas.value.filterKeys { it >= timestamp }
    }
}
