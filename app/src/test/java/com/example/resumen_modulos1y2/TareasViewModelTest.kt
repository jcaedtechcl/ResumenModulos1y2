package com.example.resumen_modulos1y2

import com.example.resumen_modulos1y2.ui.todo.TareasViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** El ViewModel no depende de Android ni de Compose, así que se prueba con un test unitario normal. */
class TareasViewModelTest {

    @Test
    fun agregar_poneLaTareaNuevaArriba_conIdSiguiente() {
        val viewModel = TareasViewModel()
        val maxIdInicial = viewModel.tareas.value.maxOf { it.id }

        viewModel.agregar("  Comprar pan  ")

        val primera = viewModel.tareas.value.first()
        assertEquals("Comprar pan", primera.titulo)
        assertEquals(maxIdInicial + 1, primera.id)
    }

    @Test
    fun agregar_ignoraTitulosVacios() {
        val viewModel = TareasViewModel()
        val cantidadInicial = viewModel.tareas.value.size

        viewModel.agregar("   ")

        assertEquals(cantidadInicial, viewModel.tareas.value.size)
    }

    @Test
    fun cambiarCompletada_yLimpiarCompletadas() {
        val viewModel = TareasViewModel()
        val id = viewModel.tareas.value.first().id

        viewModel.cambiarCompletada(id, true)
        assertTrue(viewModel.tareas.value.first { it.id == id }.completada)

        viewModel.limpiarCompletadas()
        assertTrue(viewModel.tareas.value.none { it.completada })
    }
}
