package com.example.resumen_modulos1y2.iss.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.resumen_modulos1y2.iss.data.IssRefresher
import com.example.resumen_modulos1y2.iss.data.remote.RemoteResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TestListenableWorkerBuilder crea el worker y permite llamar a doWork() directamente, sin
 * WorkManager, sin restricciones y sin esperar. Con la misma IssWorkerFactory de la app se le
 * entrega un repositorio falso que responde lo que la prueba necesita.
 */
@RunWith(AndroidJUnit4::class)
class IssSyncWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private suspend fun ejecutar(respuesta: RemoteResult<Unit>, intento: Int = 0): Result {
        val repositorioFalso = IssRefresher { respuesta }
        val worker = TestListenableWorkerBuilder<IssSyncWorker>(context)
            .setWorkerFactory(IssWorkerFactory { repositorioFalso })
            .setRunAttemptCount(intento)
            .build()
        return worker.doWork()
    }

    @Test
    fun success_esSuccess_conLaHoraDeTermino() = runTest {
        val resultado = ejecutar(RemoteResult.Success(Unit))

        assertEquals(Result.Success::class, resultado::class)
        assert((resultado as Result.Success).outputData.getLong(IssSyncWorker.KEY_TERMINADO_EN, 0) > 0)
    }

    @Test
    fun fallosTransitorios_sonRetry() = runTest {
        listOf(RemoteResult.Timeout, RemoteResult.NoConnection, RemoteResult.RateLimited, RemoteResult.HttpError(503))
            .forEach { assertEquals("$it", Result.retry(), ejecutar(it)) }
    }

    @Test
    fun fallosPermanentes_sonFailure() = runTest {
        listOf(RemoteResult.HttpError(404), RemoteResult.InvalidData)
            .forEach { assertEquals("$it", Result.failure(), ejecutar(it)) }
    }

    @Test
    fun trasElMaximoDeIntentos_unFalloTransitorioEsFailure() = runTest {
        assertEquals(Result.retry(), ejecutar(RemoteResult.Timeout, intento = IssSyncWorker.MAX_INTENTOS - 1))
        assertEquals(Result.failure(), ejecutar(RemoteResult.Timeout, intento = IssSyncWorker.MAX_INTENTOS))
    }
}
