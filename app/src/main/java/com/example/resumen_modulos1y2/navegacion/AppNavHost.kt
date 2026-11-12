package com.example.resumen_modulos1y2.navegacion

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.resumen_modulos1y2.CasoRecreacionScreen
import com.example.resumen_modulos1y2.ui.componentes.ComponentesScreen
import com.example.resumen_modulos1y2.ui.estado.EstadoScreen
import com.example.resumen_modulos1y2.ui.flow.FlowScreen
import com.example.resumen_modulos1y2.ui.layout.LayoutScreen
import com.example.resumen_modulos1y2.ui.listas.ListasScreen
import com.example.resumen_modulos1y2.ui.navegacion.NavegacionScreen
import com.example.resumen_modulos1y2.ui.navegacion.PasoConfirmarScreen
import com.example.resumen_modulos1y2.ui.navegacion.PasoTituloScreen
import com.example.resumen_modulos1y2.ui.todo.DetalleTareaRoute
import com.example.resumen_modulos1y2.ui.todo.TareasViewModel
import com.example.resumen_modulos1y2.ui.todo.TodoRoute

/**
 * Grafo de navegación de la app.
 *
 * - NavController (`navController`): ejecuta navigate()/popBackStack() y guarda la pila.
 * - NavHost: muestra el destino que está arriba de la pila.
 * - Grafo: lo que se declara dentro del NavHost (composable<Ruta> y navigation<Grafo>).
 *
 * `tareasViewModel` se crea aquí, fuera de los destinos, así que pertenece a la Activity y
 * la lista, el detalle y el asistente comparten la misma instancia (y el mismo StateFlow).
 */
@Composable
fun AppNavHost(
    recreaciones: Int,
    onSimularRotacion: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    tareasViewModel: TareasViewModel = viewModel()
) {
    val volver: () -> Unit = { navController.popBackStack() }
    val pila = rememberPilaNavegacion(navController)

    NavHost(
        navController = navController,
        startDestination = RutaTareas,
        modifier = modifier
    ) {
        composable<RutaTareas> {
            TodoRoute(
                viewModel = tareasViewModel,
                onAbrirTarea = { id -> navController.navigate(RutaDetalleTarea(id)) },
                onAbrirLeccion = { leccion -> navController.navigate(leccion.ruta()) }
            )
        }

        composable<RutaDetalleTarea> { backStackEntry ->
            // toRoute() reconstruye la data class con los argumentos que vinieron en la ruta
            val ruta: RutaDetalleTarea = backStackEntry.toRoute()
            DetalleTareaRoute(id = ruta.id, viewModel = tareasViewModel, onVolver = volver)
        }

        // Lecciones
        composable<RutaRecreacion> {
            CasoRecreacionScreen(
                recreaciones = recreaciones,
                onSimularRotacion = onSimularRotacion,
                onVolver = volver
            )
        }
        composable<RutaLayout> { LayoutScreen(onVolver = volver) }
        composable<RutaComponentes> { ComponentesScreen(onVolver = volver) }
        composable<RutaEstado> { EstadoScreen(onVolver = volver) }
        composable<RutaListas> { ListasScreen(onVolver = volver) }
        composable<RutaFlow> { FlowScreen(onVolver = volver) }
        composable<RutaNavegacion> {
            val tareas by tareasViewModel.tareas.collectAsStateWithLifecycle()
            NavegacionScreen(
                pila = pila,
                tareas = tareas,
                onAbrirTarea = { id -> navController.navigate(RutaDetalleTarea(id)) },
                onIniciarAsistente = { navController.navigate(GrafoNuevaTarea) },
                onAbrirOtraVez = { navController.navigate(RutaNavegacion) },
                onAbrirOtraVezSingleTop = {
                    navController.navigate(RutaNavegacion) { launchSingleTop = true }
                },
                onVolver = volver
            )
        }

        // Subgrafo anidado: se entra con navigate(GrafoNuevaTarea) y empieza en RutaPasoTitulo
        navigation<GrafoNuevaTarea>(startDestination = RutaPasoTitulo) {
            composable<RutaPasoTitulo> {
                PasoTituloScreen(
                    pila = pila,
                    onSiguiente = { titulo -> navController.navigate(RutaPasoConfirmar(titulo)) },
                    // Sale de todo el subgrafo de una vez
                    onCancelar = { navController.popBackStack<GrafoNuevaTarea>(inclusive = true) }
                )
            }
            composable<RutaPasoConfirmar> { backStackEntry ->
                val ruta: RutaPasoConfirmar = backStackEntry.toRoute()
                PasoConfirmarScreen(
                    titulo = ruta.titulo,
                    pila = pila,
                    onGuardar = {
                        tareasViewModel.agregar(ruta.titulo)
                        // Vuelve a la lista quitando de la pila todo lo que había encima
                        navController.navigate(RutaTareas) {
                            popUpTo<RutaTareas> { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onAtras = volver
                )
            }
        }
    }
}

/**
 * Nombres cortos de los destinos en la pila, para mostrarlos en pantalla.
 * La pila es un StateFlow del NavController, así que también se observa con
 * collectAsStateWithLifecycle.
 *
 * Navigation no ofrece una API pública para leer la pila completa: `currentBackStack` está
 * marcada como interna de la librería. Aquí se usa solo para visualizarla en la lección; en
 * una app real se usa currentBackStackEntryAsState() para conocer el destino actual.
 */
@SuppressLint("RestrictedApi")
@Composable
private fun rememberPilaNavegacion(navController: NavHostController): List<String> {
    val pila by navController.currentBackStack.collectAsStateWithLifecycle()
    return pila
        .filter { it.destination.parent != null } // omite el grafo raíz del NavHost
        .mapNotNull { entrada ->
            entrada.destination.route
                ?.substringBefore('/')
                ?.substringBefore('?')
                ?.substringAfterLast('.')
        }
}
