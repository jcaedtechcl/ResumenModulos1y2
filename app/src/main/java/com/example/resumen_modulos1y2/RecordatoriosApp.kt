package com.example.resumen_modulos1y2

import android.app.Application
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
