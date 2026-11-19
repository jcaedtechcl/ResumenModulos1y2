package com.example.resumen_modulos1y2.data.local

import com.example.resumen_modulos1y2.model.Prioridad
import com.example.resumen_modulos1y2.model.Recordatorio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Mapeadores y TypeConverter son funciones puras: se prueban sin base de datos. */
class ReminderMappersTest {

    private val recordatorio = Recordatorio(
        id = "a1b2",
        titulo = "Pagar la luz",
        descripcion = "Antes del día 10",
        categoria = "Hogar",
        prioridad = Prioridad.Alta,
        completado = true,
        fecha = LocalDate.of(2026, 12, 10)
    )

    @Test
    fun idaYVuelta_conservaTodosLosCampos() {
        val entidad = recordatorio.toEntity(updatedAt = 123L)

        assertEquals("due_at se guarda como número", 1_796_860_800_000L, entidad.dueAt)
        assertEquals(123L, entidad.updatedAt)
        assertEquals(recordatorio, entidad.toDomain())
    }

    @Test
    fun sinFechaNiDescripcion_quedanNull() {
        val entidad = Recordatorio(id = "x", titulo = "Sin fecha").toEntity(updatedAt = 0L)

        assertNull(entidad.dueAt)
        assertNull(entidad.description)
        assertNull(entidad.toDomain().fecha)
    }

    @Test
    fun converter_guardaElNombre_yLeeUnoDesconocidoComoMedia() {
        val converters = Converters()

        assertEquals("Alta", converters.prioridadToString(Prioridad.Alta))
        assertEquals(Prioridad.Baja, converters.stringToPrioridad("Baja"))
        assertEquals(Prioridad.Media, converters.stringToPrioridad("URGENTE"))
    }
}
