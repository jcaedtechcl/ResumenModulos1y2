package com.example.resumen_modulos1y2.iss.data

import com.example.resumen_modulos1y2.iss.data.local.IssPositionEntity
import com.example.resumen_modulos1y2.iss.data.remote.IssPositionDto
import com.example.resumen_modulos1y2.iss.domain.IssPosition
import com.example.resumen_modulos1y2.iss.domain.Visibilidad
import java.time.Instant

/*
 * El camino de los datos tiene tres modelos y dos traducciones:
 *
 *   IssPositionDto ──toEntity()──► IssPositionEntity ──toDomain()──► IssPosition
 *   (contrato con la API)          (fila de Room)                    (lo que ve la UI)
 *
 * La red solo escribe en Room y la UI solo lee de Room. Por eso no existe DTO → dominio: lo que
 * se muestra siempre pasó por la base.
 */

fun IssPositionDto.toEntity(): IssPositionEntity = IssPositionEntity(
    timestamp = timestampSeconds,
    latitude = latitude,
    longitude = longitude,
    altitudeKm = altitude,
    velocityKmh = velocity,
    visibility = visibility
)

fun IssPositionEntity.toDomain(): IssPosition = IssPosition(
    latitude = latitude,
    longitude = longitude,
    altitudeKm = altitudeKm,
    velocityKmh = velocityKmh,
    visibilidad = visibilidadDesde(visibility),
    medidaEn = Instant.ofEpochSecond(timestamp)
)

// Un valor nuevo en la API no debe romper la app: se muestra como "desconocida"
internal fun visibilidadDesde(texto: String): Visibilidad = when (texto.lowercase()) {
    "daylight" -> Visibilidad.LUZ_SOLAR
    "eclipsed" -> Visibilidad.ECLIPSE
    else -> Visibilidad.DESCONOCIDA
}
