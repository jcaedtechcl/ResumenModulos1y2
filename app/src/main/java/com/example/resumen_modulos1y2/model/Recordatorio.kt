package com.example.resumen_modulos1y2.model

import java.time.LocalDate

/**
 * Recordatorio del Módulo 3. Vive solo en memoria: persistir recordatorios es tarea de Room
 * (tópico siguiente). DataStore guarda únicamente las preferencias de cómo se muestran.
 */
data class Recordatorio(
    val id: Long,
    val titulo: String,
    val fecha: LocalDate,
    val prioridad: Prioridad,
    val completado: Boolean = false
)

/**
 * Datos de ejemplo relativos a `hoy`, elegidos para que ordenar por fecha y por prioridad
 * dé resultados distintos y para que haya completados que ocultar.
 */
fun recordatoriosDeEjemplo(hoy: LocalDate = LocalDate.now()): List<Recordatorio> = listOf(
    Recordatorio(1, "Pagar la cuenta de la luz", hoy.plusDays(5), Prioridad.Alta),
    Recordatorio(2, "Llamar al dentista", hoy.plusDays(1), Prioridad.Media),
    Recordatorio(3, "Regar las plantas", hoy, Prioridad.Baja, completado = true),
    Recordatorio(4, "Entregar la Actividad 3", hoy.plusDays(2), Prioridad.Alta),
    Recordatorio(5, "Comprar pan", hoy.plusDays(3), Prioridad.Baja),
    Recordatorio(6, "Renovar la credencial", hoy.minusDays(1), Prioridad.Media, completado = true)
)
