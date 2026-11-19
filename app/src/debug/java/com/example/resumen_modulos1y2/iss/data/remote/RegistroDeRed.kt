package com.example.resumen_modulos1y2.iss.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Versión DEBUG (src/debug): registra en Logcat cada solicitud y respuesta completa (nivel BODY),
 * con la etiqueta "okhttp.OkHttpClient".
 *
 * En src/release existe otra versión con la misma firma que no hace nada, y Gradle compila solo
 * la del build en curso. Es más fuerte que un `if (BuildConfig.DEBUG)`: como logging-interceptor
 * es `debugImplementation`, el APK de release ni siquiera contiene la clase. No hay forma de que
 * el registro se active en producción por un error en una condición.
 */
fun OkHttpClient.Builder.agregarRegistroDeRed(): OkHttpClient.Builder =
    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
