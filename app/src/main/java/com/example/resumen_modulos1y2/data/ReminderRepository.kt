package com.example.resumen_modulos1y2.data

import com.example.resumen_modulos1y2.data.local.ReminderDao
import com.example.resumen_modulos1y2.data.local.ReminderEntity
import com.example.resumen_modulos1y2.data.local.toDomain
import com.example.resumen_modulos1y2.data.local.toEntity
import com.example.resumen_modulos1y2.model.Recordatorio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Único punto de acceso a los recordatorios guardados. Hacia afuera solo habla de Recordatorio:
 * el ViewModel no sabe que existen Room, el DAO ni ReminderEntity.
 *
 * Camino de los datos: Compose → ViewModel → ReminderRepository → ReminderDao → SQLite.
 *
 * `reloj` se recibe como parámetro para poder fijar la hora en una prueba; en la app es la hora
 * del sistema.
 */
class ReminderRepository(
    private val dao: ReminderDao,
    private val reloj: () -> Long = System::currentTimeMillis
) {

    // Cada vez que la tabla cambia, el DAO emite una lista nueva y aquí se traduce al dominio
    fun observeAll(): Flow<List<Recordatorio>> = dao.observeAll().map { it.aDominio() }

    fun observeByCompleted(completado: Boolean): Flow<List<Recordatorio>> =
        dao.observeByCompleted(completado).map { it.aDominio() }

    suspend fun guardar(recordatorio: Recordatorio) {
        dao.save(recordatorio.toEntity(updatedAt = reloj()))
    }

    suspend fun completar(id: String, completado: Boolean) {
        dao.updateCompleted(id, completado, updatedAt = reloj())
    }

    // @Delete identifica la fila por su clave primaria; el resto de los campos no importa
    suspend fun eliminar(recordatorio: Recordatorio) {
        dao.delete(recordatorio.toEntity(updatedAt = reloj()))
    }

    private fun List<ReminderEntity>.aDominio(): List<Recordatorio> = map { it.toDomain() }
}
