package com.example.resumen_modulos1y2.model

import java.time.LocalDate

/**
 * Recordatorio tal como lo conoce la app (modelo de dominio). Las pantallas y los ViewModels
 * trabajan SIEMPRE con esta clase; cómo se guarda en la base de datos (ReminderEntity) es un
 * detalle de data/local.
 *
 * `id` es un UUID en texto: se genera al crear el recordatorio, antes de guardarlo, así el
 * objeto ya tiene identidad sin esperar a que la base asigne un número.
 */
data class Recordatorio(
    val id: String,
    val titulo: String,
    val descripcion: String? = null,
    val categoria: String = CATEGORIA_GENERAL,
    val prioridad: Prioridad = Prioridad.Media,
    val completado: Boolean = false,
    val fecha: LocalDate? = null,
    // Se guarda desde la versión 2 de la base; la interfaz todavía no lo usa
    val archivado: Boolean = false
)

const val CATEGORIA_GENERAL = "General"

/**
 * Datos de ejemplo para vistas previas y pruebas. Relativos a `hoy` y elegidos para que ordenar
 * por fecha y por prioridad dé resultados distintos y para que haya completados que ocultar.
 */
fun recordatoriosDeEjemplo(hoy: LocalDate = LocalDate.now()): List<Recordatorio> = listOf(
    Recordatorio("1", "Pagar la cuenta de la luz", categoria = "Hogar", prioridad = Prioridad.Alta, fecha = hoy.plusDays(5)),
    Recordatorio("2", "Llamar al dentista", categoria = "Salud", prioridad = Prioridad.Media, fecha = hoy.plusDays(1)),
    Recordatorio("3", "Regar las plantas", categoria = "Hogar", prioridad = Prioridad.Baja, completado = true, fecha = hoy),
    Recordatorio("4", "Entregar la Actividad 3", "Consigna 2: Room", "Estudio", Prioridad.Alta, fecha = hoy.plusDays(2)),
    Recordatorio("5", "Comprar pan", prioridad = Prioridad.Baja, fecha = hoy.plusDays(3)),
    Recordatorio("6", "Renovar la credencial", prioridad = Prioridad.Media, completado = true, fecha = hoy.minusDays(1)),
    Recordatorio("7", "Ordenar fotos", prioridad = Prioridad.Baja)
)
