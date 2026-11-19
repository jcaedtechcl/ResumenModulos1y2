package com.example.resumen_modulos1y2.data.local

import com.example.resumen_modulos1y2.model.Recordatorio
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Traducción entre la fila de la base (ReminderEntity) y el modelo de la app (Recordatorio).
// Son funciones puras: se prueban con JUnit, sin base de datos.

fun ReminderEntity.toDomain(): Recordatorio = Recordatorio(
    id = id,
    titulo = title,
    descripcion = description,
    categoria = category,
    prioridad = priority,
    completado = completed,
    fecha = dueAt?.toLocalDate(),
    archivado = archived
)

/**
 * `updatedAt` llega como parámetro porque el dominio no lo guarda: es un dato de
 * almacenamiento que pone el repositorio con la hora del momento en que guarda.
 */
fun Recordatorio.toEntity(updatedAt: Long): ReminderEntity = ReminderEntity(
    id = id,
    title = titulo,
    description = descripcion,
    category = categoria,
    priority = prioridad,
    completed = completado,
    dueAt = fecha?.toEpochMillis(),
    updatedAt = updatedAt,
    archived = archivado
)

// La fecha se guarda como milisegundos del inicio del día en UTC. Usar UTC en ambos sentidos
// garantiza que la misma fecha vuelve igual aunque el teléfono cambie de zona horaria.
private fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
