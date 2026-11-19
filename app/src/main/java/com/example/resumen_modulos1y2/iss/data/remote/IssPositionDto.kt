package com.example.resumen_modulos1y2.iss.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO (Data Transfer Object): copia exacta del JSON que entrega la API. Es un CONTRATO con el
 * servidor, no un modelo de la app. Si la API cambia un nombre, solo se toca esta clase.
 *
 * Respuesta real (verificada con curl el 19-11-2026):
 * {"name":"iss","id":25544,"latitude":-20.66,"longitude":-154.20,"altitude":422.19,
 *  "velocity":27572.64,"visibility":"daylight","footprint":4518.59,"timestamp":1795124992,
 *  "daynum":2461364.40,"solar_lat":-19.61,"solar_lon":208.89,"units":"kilometers"}
 *
 * - `@JsonClass(generateAdapter = true)`: Moshi genera el adaptador al compilar (KSP), sin
 *   reflexión. Es más rápido, y si algo no calza falla al compilar, no en el teléfono del usuario.
 * - `@Json(name = …)`: el nombre del JSON manda; la propiedad Kotlin puede llamarse distinto.
 * - Los campos que no se usan (footprint, daynum, solar_lat…) simplemente no se declaran: Moshi
 *   los ignora.
 */
@JsonClass(generateAdapter = true)
data class IssPositionDto(
    @Json(name = "name") val name: String,
    @Json(name = "id") val id: Int,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "altitude") val altitude: Double,
    @Json(name = "velocity") val velocity: Double,
    @Json(name = "visibility") val visibility: String,
    // Segundos desde 1970 (Unix), NO milisegundos
    @Json(name = "timestamp") val timestampSeconds: Long,
    @Json(name = "units") val units: String
)
