package com.example.resumen_modulos1y2.iss.domain

import java.time.Instant

/** Si la ISS está iluminada por el Sol o en la sombra de la Tierra. */
enum class Visibilidad { LUZ_SOLAR, ECLIPSE, DESCONOCIDA }

/**
 * Modelo de DOMINIO: lo que la app entiende por "posición de la ISS". No sabe de JSON, de
 * Retrofit ni de Room. La UI solo conoce esta clase; el DTO nunca sale de la capa de datos.
 *
 * Las unidades van en el nombre (Km, Kmh) para que nadie tenga que adivinarlas.
 */
data class IssPosition(
    val latitude: Double,
    val longitude: Double,
    val altitudeKm: Double,
    val velocityKmh: Double,
    val visibilidad: Visibilidad,
    val medidaEn: Instant
)
