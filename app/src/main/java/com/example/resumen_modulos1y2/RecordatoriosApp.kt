package com.example.resumen_modulos1y2

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.resumen_modulos1y2.di.AppContainer

/**
 * Clase Application: Android crea una sola instancia, antes que cualquier Activity, y la
 * mantiene mientras vive el proceso. Por eso es el lugar del contenedor de dependencias.
 * Se registra en el AndroidManifest con android:name=".RecordatoriosApp".
 */
class RecordatoriosApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Atajo para las fábricas de ViewModel: Android entrega la Application en las CreationExtras
 * y de ella se obtiene el contenedor.
 */
fun CreationExtras.appContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RecordatoriosApp).container
