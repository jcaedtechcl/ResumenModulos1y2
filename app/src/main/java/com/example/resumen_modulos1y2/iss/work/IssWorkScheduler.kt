package com.example.resumen_modulos1y2.iss.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.resumen_modulos1y2.iss.domain.EstadoSincronizacion
import com.example.resumen_modulos1y2.iss.domain.EstadoTrabajo
import com.example.resumen_modulos1y2.iss.domain.FaseTrabajo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Lo que el ViewModel necesita del trabajo en segundo plano. Es una interfaz para que el
 * ViewModel se pruebe sin WorkManager.
 */
interface BackgroundSync {
    val estado: Flow<EstadoSincronizacion>
    fun programarPeriodico()
    fun actualizarAhora()
}

/**
 * Programa los trabajos de IssSyncWorker.
 *
 * ¿WorkManager o una coroutine? Una coroutine (viewModelScope, el bucle de la etapa 4) muere con
 * la pantalla o con el proceso. WorkManager guarda el trabajo en su propia base de datos y el
 * SISTEMA lo ejecuta aunque la app esté cerrada o el teléfono se haya reiniciado. Se usa para
 * trabajo que DEBE ocurrir tarde o temprano, no para trabajo inmediato mientras se mira la pantalla.
 */
class IssWorkScheduler(context: Context) : BackgroundSync {

    private val workManager = WorkManager.getInstance(context)

    // Restricción: solo con red. Sin red, el trabajo espera en cola en vez de fallar
    private val conRed = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Cada 15 min, el MÍNIMO que permite el sistema (con menos, WorkManager usa 15 igual).
     * Los tiempos NO son exactos: en Doze el sistema agrupa los trabajos en "ventanas de
     * mantenimiento" para ahorrar batería, y la ejecución se puede atrasar varios minutos o más.
     *
     * enqueueUniquePeriodicWork + KEEP: si ya existe un trabajo con ese nombre, se conserva y este
     * se descarta. Por eso se puede llamar en cada arranque de la app sin duplicar nada.
     */
    override fun programarPeriodico() {
        val solicitud = PeriodicWorkRequestBuilder<IssSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(conRed)
            // Tras un retry() se espera 30 s, luego 60, 120… (exponencial, hasta 5 h como máximo)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(TRABAJO_PERIODICO, ExistingPeriodicWorkPolicy.KEEP, solicitud)
    }

    /**
     * Botón "Actualizar en segundo plano". Con ExistingWorkPolicy.KEEP, si ya hay uno en cola o
     * en ejecución, los toques extra se ignoran: cinco toques = un solo trabajo.
     */
    override fun actualizarAhora() {
        val solicitud = OneTimeWorkRequestBuilder<IssSyncWorker>()
            .setConstraints(conRed)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(TRABAJO_INMEDIATO, ExistingWorkPolicy.KEEP, solicitud)
    }

    /** WorkManager informa cada cambio de estado; aquí se traduce a un modelo propio. */
    override val estado: Flow<EstadoSincronizacion> = combine(
        workManager.getWorkInfosForUniqueWorkFlow(TRABAJO_PERIODICO),
        workManager.getWorkInfosForUniqueWorkFlow(TRABAJO_INMEDIATO)
    ) { periodico, inmediato ->
        EstadoSincronizacion(
            periodico = periodico.lastOrNull().aEstadoTrabajo(),
            inmediato = inmediato.lastOrNull().aEstadoTrabajo()
        )
    }

    companion object {
        const val TRABAJO_PERIODICO = "iss_periodic_sync"
        const val TRABAJO_INMEDIATO = "iss_sync_now"
    }
}

internal fun WorkInfo?.aEstadoTrabajo(): EstadoTrabajo {
    if (this == null) return EstadoTrabajo(FaseTrabajo.SIN_PROGRAMAR)
    // nextScheduleTimeMillis = Long.MAX_VALUE cuando no hay una próxima ejecución prevista
    val proxima = nextScheduleTimeMillis.takeIf { it != Long.MAX_VALUE }?.let(Instant::ofEpochMilli)
    return when (state) {
        // Si ya falló al menos una vez, está esperando el backoff para reintentar
        WorkInfo.State.ENQUEUED ->
            EstadoTrabajo(if (runAttemptCount > 0) FaseTrabajo.REINTENTANDO else FaseTrabajo.EN_COLA, proxima)
        WorkInfo.State.RUNNING -> EstadoTrabajo(FaseTrabajo.EN_EJECUCION)
        WorkInfo.State.SUCCEEDED -> EstadoTrabajo(
            FaseTrabajo.COMPLETADO,
            outputData.getLong(IssSyncWorker.KEY_TERMINADO_EN, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli)
        )
        WorkInfo.State.FAILED -> EstadoTrabajo(FaseTrabajo.FALLIDO)
        WorkInfo.State.BLOCKED -> EstadoTrabajo(FaseTrabajo.EN_COLA)
        WorkInfo.State.CANCELLED -> EstadoTrabajo(FaseTrabajo.CANCELADO)
    }
}
