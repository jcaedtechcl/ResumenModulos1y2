package com.example.resumen_modulos1y2.di

import android.content.Context
import androidx.room.Room
import com.example.resumen_modulos1y2.BuildConfig
import com.example.resumen_modulos1y2.data.ReminderRepository
import com.example.resumen_modulos1y2.data.local.AppDatabase
import com.example.resumen_modulos1y2.data.local.MIGRATION_1_2
import com.example.resumen_modulos1y2.data.local.MIGRATION_2_3
import com.example.resumen_modulos1y2.data.preferences.UserPreferencesRepository
import com.example.resumen_modulos1y2.data.preferences.userPreferencesDataStore
import com.example.resumen_modulos1y2.iss.data.AndroidConnectivityObserver
import com.example.resumen_modulos1y2.iss.data.ConnectivityObserver
import com.example.resumen_modulos1y2.iss.data.IssRepository
import com.example.resumen_modulos1y2.iss.data.remote.IssApiService
import com.example.resumen_modulos1y2.iss.data.remote.agregarRegistroDeRed
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Inyección de dependencias manual: un solo lugar crea las dependencias compartidas y el resto
 * de la app las pide aquí en vez de construirlas por su cuenta.
 *
 * Se usa `applicationContext` porque el DataStore vive tanto como la app. Si se guardara el
 * contexto de una Activity, al rotar la pantalla quedaría una referencia a la Activity
 * destruida (fuga de memoria).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // El DataStore ya no se expone: desde esta etapa se accede solo a través del repositorio
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext.userPreferencesDataStore)
    }

    // Única instancia de la base de datos. `lazy` la abre recién cuando alguien la pide, no al
    // arrancar la app. Es privada: el resto de la app usa el repositorio.
    private val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.NOMBRE_ARCHIVO)
            // NO se usa fallbackToDestructiveMigration(): si faltara una migración, Room BORRARÍA
            // la base y la crearía vacía. Los recordatorios del usuario se perderían en silencio
            // al actualizar la app. Sin ese "plan B", una migración faltante es un error visible
            // durante el desarrollo, que es cuando se debe descubrir.
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(appDatabase.reminderDao())
    }

    // ---------------------------------------------------------------------------------------
    // M3 ISS: red
    //
    // UN SOLO OkHttpClient y UN SOLO Retrofit para toda la app. Cada cliente tiene su propio
    // pool de conexiones y de hilos: crear uno por pantalla (o por llamada) desperdicia memoria
    // y no reutiliza conexiones HTTPS ya abiertas.
    // ---------------------------------------------------------------------------------------

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Cuánto esperar para ABRIR la conexión y para RECIBIR datos. Sin límite, una red
            // mala dejaría la pantalla cargando indefinidamente.
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            // Registro de solicitudes en Logcat: solo existe en debug (ver RegistroDeRed.kt)
            .agregarRegistroDeRed()
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            // La URL viene de gradle.properties a través de BuildConfig: no está en el código
            .baseUrl(BuildConfig.ISS_BASE_URL)
            .client(okHttpClient)
            // Moshi sin reflexión: usa los adaptadores que KSP generó para los DTO
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
    }

    private val issApiService: IssApiService by lazy { retrofit.create(IssApiService::class.java) }

    // La red escribe en Room y la pantalla lee de Room (ver IssRepository)
    val issRepository: IssRepository by lazy { IssRepository(issApiService, appDatabase.issPositionDao()) }

    val connectivityObserver: ConnectivityObserver by lazy { AndroidConnectivityObserver(appContext) }
}
