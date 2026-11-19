package com.example.resumen_modulos1y2.data.local

import androidx.room.TypeConverter
import com.example.resumen_modulos1y2.model.Prioridad

/**
 * Room solo sabe guardar tipos simples (números, texto, bytes). Un TypeConverter le enseña a
 * convertir un tipo propio hacia y desde uno de ellos. Se registra en AppDatabase con
 * @TypeConverters.
 */
class Converters {

    // Se guarda el `name` ("Alta", "Media", "Baja") y no el `ordinal`: si mañana se reordena o
    // se agrega una constante al enum, los datos ya guardados siguen significando lo mismo.
    @TypeConverter
    fun prioridadToString(prioridad: Prioridad): String = prioridad.name

    // Igual que en DataStore: firstOrNull en vez de valueOf, para que un texto desconocido
    // (dato antiguo, enum renombrado) se lea como Media en vez de cerrar la app.
    @TypeConverter
    fun stringToPrioridad(valor: String): Prioridad =
        Prioridad.entries.firstOrNull { it.name == valor } ?: Prioridad.Media
}
