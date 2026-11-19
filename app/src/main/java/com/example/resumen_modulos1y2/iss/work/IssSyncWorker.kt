package com.example.resumen_modulos1y2.iss.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.resumen_modulos1y2.iss.data.IssRefresher
import com.example.resumen_modulos1y2.iss.data.remote.RemoteResult

/**
 * Trabajo en segundo plano: pide UNA actualización al repositorio y le dice a WorkManager cómo
 * le fue.
 *
 * CoroutineWorker: `doWork` es `suspend` y corre en Dispatchers.Default, fuera del hilo
 * principal. Si el sistema cancela el trabajo (se perdió la red, la batería está baja…), la
 * coroutine se cancela y la solicitud en curso también.
 *
 * El worker no sabe de Retrofit ni de Room: recibe un IssRefresher, que en la app es el mismo
 * IssRepository que usa la pantalla (con el mismo Mutex), y en la prueba es uno falso.
 */
class IssSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val refresher: IssRefresher,
    private val reloj: () -> Long = System::currentTimeMillis
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val resultado = refresher.refresh()
        // Con fallos transitorios se reintenta, pero no para siempre: el trabajo inmediato
        // terminaría reintentando durante horas. El periódico vuelve a correr en 15 min igual.
        if (runAttemptCount >= MAX_INTENTOS) {
            return if (resultado is RemoteResult.Success) exito() else Result.failure()
        }
        return when (resultado) {
            is RemoteResult.Success -> exito()
            // Transitorios: más tarde puede funcionar → retry() con backoff exponencial
            RemoteResult.Timeout, RemoteResult.NoConnection, RemoteResult.RateLimited -> Result.retry()
            is RemoteResult.HttpError -> if (resultado.esErrorDelServidor) Result.retry() else Result.failure()
            // Permanentes: repetir la misma solicitud dará el mismo error → failure()
            RemoteResult.InvalidData -> Result.failure()
        }
    }

    // La hora de término viaja en outputData: la pantalla la muestra junto al estado
    private fun exito(): Result = Result.success(workDataOf(KEY_TERMINADO_EN to reloj()))

    companion object {
        const val KEY_TERMINADO_EN = "terminado_en"
        const val MAX_INTENTOS = 5
    }
}

/**
 * WorkManager crea los workers por su cuenta, con reflexión, y solo sabe pasarles (Context,
 * WorkerParameters). Para entregarle al worker el repositorio de AppContainer se registra esta
 * fábrica en la configuración de WorkManager (ver RecordatoriosApp). Es inyección manual de
 * dependencias, igual que las fábricas de ViewModel.
 *
 * Devolver null para otras clases deja que WorkManager use su fábrica por defecto.
 */
class IssWorkerFactory(private val refresher: () -> IssRefresher) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? = when (workerClassName) {
        IssSyncWorker::class.java.name -> IssSyncWorker(appContext, workerParameters, refresher())
        else -> null
    }
}
