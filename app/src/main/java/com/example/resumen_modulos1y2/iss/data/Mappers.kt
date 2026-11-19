package com.example.resumen_modulos1y2.iss.data

import com.example.resumen_modulos1y2.iss.data.remote.IssPositionDto
import com.example.resumen_modulos1y2.iss.domain.IssPosition
import com.example.resumen_modulos1y2.iss.domain.Visibilidad
import java.time.Instant

/**
 * Traducción DTO → dominio. Aquí se interpretan los textos de la API ("daylight", "eclipsed") y
 * los segundos Unix. El resto de la app recibe tipos claros.
 */
fun IssPositionDto.toDomain(): IssPosition = IssPosition(
    latitude = latitude,
    longitude = longitude,
    altitudeKm = altitude,
    velocityKmh = velocity,
    visibilidad = visibilidadDesde(visibility),
    medidaEn = Instant.ofEpochSecond(timestampSeconds)
)

// Un valor nuevo en la API no debe romper la app: se muestra como "desconocida"
internal fun visibilidadDesde(texto: String): Visibilidad = when (texto.lowercase()) {
    "daylight" -> Visibilidad.LUZ_SOLAR
    "eclipsed" -> Visibilidad.ECLIPSE
    else -> Visibilidad.DESCONOCIDA
}
