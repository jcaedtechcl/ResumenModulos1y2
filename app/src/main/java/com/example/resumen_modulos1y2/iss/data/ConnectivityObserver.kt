package com.example.resumen_modulos1y2.iss.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * "¿Hay red?" como un Flow. Es una interfaz para que el ViewModel se pruebe con un Flow falso.
 *
 * Conectividad es una SEÑAL, no una garantía: "hay red" no asegura que la API responda (puede
 * estar caída, o el Wi-Fi puede pedir iniciar sesión). Por eso la app igual trata cada fallo
 * con RemoteResult. Se usa para no intentar en vano y para reaccionar cuando la red vuelve.
 */
interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
}

/**
 * Implementación con ConnectivityManager.
 *
 * callbackFlow convierte una API de callbacks (registrar → recibir avisos → desregistrar) en un
 * Flow:
 * - al empezar a recolectar, se registra el callback;
 * - cada aviso del sistema se envía con trySend;
 * - awaitClose se ejecuta cuando el recolector se cancela (por ejemplo, al salir de la pantalla) y
 *   desregistra el callback. Si se olvida, el callback sigue vivo y produce una fuga.
 */
class AndroidConnectivityObserver(context: Context) : ConnectivityObserver {

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    override val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.tieneInternet())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        // Estado inicial: si no hay ninguna red, el sistema no llamará a ningún callback y el
        // Flow quedaría sin valor. Por eso se emite primero el estado actual.
        trySend(estadoActual())
        // "Default" = la red que la app usaría ahora (Wi-Fi, datos móviles…), no todas las redes
        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        // El sistema avisa varias veces por la misma red (por ejemplo, cuando cambia la señal).
        // Solo interesan los CAMBIOS: true → false o false → true.
        .distinctUntilChanged()

    private fun estadoActual(): Boolean {
        val red = connectivityManager.activeNetwork ?: return false
        return connectivityManager.getNetworkCapabilities(red)?.tieneInternet() == true
    }

    // INTERNET: la red dice ofrecer internet. VALIDATED: el sistema comprobó que de verdad llega.
    private fun NetworkCapabilities.tieneInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
