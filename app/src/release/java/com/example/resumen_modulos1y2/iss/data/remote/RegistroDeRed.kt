package com.example.resumen_modulos1y2.iss.data.remote

import okhttp3.OkHttpClient

/**
 * Versión RELEASE (src/release): no agrega ningún interceptor de registro. En producción, un
 * registro con nivel BODY escribiría en Logcat el contenido de cada respuesta. Ver la versión de
 * src/debug.
 */
fun OkHttpClient.Builder.agregarRegistroDeRed(): OkHttpClient.Builder = this
