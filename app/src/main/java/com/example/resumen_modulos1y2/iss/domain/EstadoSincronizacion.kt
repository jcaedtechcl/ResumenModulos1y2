package com.example.resumen_modulos1y2.iss.domain

import java.time.Instant

/** En qué está un trabajo en segundo plano, en términos que la pantalla entiende. */
enum class FaseTrabajo {
    SIN_PROGRAMAR,
    EN_COLA,
    EN_EJECUCION,
    REINTENTANDO,
    COMPLETADO,
    FALLIDO,
    CANCELADO
}

/**
 * Estado de un trabajo. `hora` depende de la fase: hora de término si está COMPLETADO, o próxima
 * ejecución prevista si está EN_COLA o REINTENTANDO.
 *
 * Es un modelo propio, no WorkInfo, por la misma razón que IssPosition no es un DTO: la pantalla
 * no debe depender de WorkManager.
 */
data class EstadoTrabajo(val fase: FaseTrabajo, val hora: Instant? = null)

data class EstadoSincronizacion(
    val periodico: EstadoTrabajo = EstadoTrabajo(FaseTrabajo.SIN_PROGRAMAR),
    val inmediato: EstadoTrabajo = EstadoTrabajo(FaseTrabajo.SIN_PROGRAMAR)
)
