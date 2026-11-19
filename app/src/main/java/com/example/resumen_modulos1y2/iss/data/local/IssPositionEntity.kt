package com.example.resumen_modulos1y2.iss.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una fila de la tabla `iss_positions`: una medición guardada.
 *
 * La clave primaria es `timestamp`, el instante de la medición en segundos Unix según la API.
 * Dos consultas que reciben la MISMA medición (por ejemplo, dos toques dentro del mismo segundo)
 * producen la misma clave, y @Upsert reemplaza la fila en vez de duplicarla. La propia medición
 * hace de identificador natural.
 *
 * `visibility` guarda el texto de la API ("daylight", "eclipsed") y se interpreta al pasar al
 * dominio. Así, si la API agrega un valor nuevo, la base no pierde información.
 */
@Entity(tableName = "iss_positions")
data class IssPositionEntity(
    @PrimaryKey
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "altitude_km") val altitudeKm: Double,
    @ColumnInfo(name = "velocity_kmh") val velocityKmh: Double,
    @ColumnInfo(name = "visibility") val visibility: String
)
