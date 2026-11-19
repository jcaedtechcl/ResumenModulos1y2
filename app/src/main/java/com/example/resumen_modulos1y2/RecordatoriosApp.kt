package com.example.resumen_modulos1y2

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.work.Configuration
import com.example.resumen_modulos1y2.di.AppContainer
import com.example.resumen_modulos1y2.iss.work.IssWorkerFactory

/**
 * Clase Application: Android crea una sola instancia, antes que cualquier Activity, y la
 * mantiene mientras vive el proceso. Por eso es el lugar del contenedor de dependencias.
 * Se registra en el AndroidManifest con android:name=".RecordatoriosApp".
 *
 * Configuration.Provider (M3 ISS, etapa 5): WorkManager se inicializa bajo demanda con esta
 * configuración, que incluye la fábrica de workers con las dependencias de AppContainer. Para
 * eso, el AndroidManifest desactiva la inicialización automática de WorkManager.
 */
class RecordatoriosApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Se puede llamar en CADA arranque: con KEEP, si el trabajo periódico ya existe, no se
        // crea otro. Abrir la app cien veces deja un solo trabajo programado.
        container.issWorkScheduler.programarPeriodico()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(IssWorkerFactory { container.issRepository })
            .build()
}

/**
 * Atajo para las fábricas de ViewModel: Android entrega la Application en las CreationExtras
 * y de ella se obtiene el contenedor.
 */
fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RecordatoriosApp).container
