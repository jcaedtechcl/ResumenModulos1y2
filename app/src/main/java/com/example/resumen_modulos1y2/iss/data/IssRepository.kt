package com.example.resumen_modulos1y2.iss.data

import com.example.resumen_modulos1y2.iss.data.remote.IssApiService
import com.example.resumen_modulos1y2.iss.domain.IssPosition
import retrofit2.HttpException

/**
 * ETAPA 1 (versión deliberadamente simple): pide la posición a la API y la devuelve.
 *
 * Problemas que resuelven las etapas siguientes:
 * - si no hay red, lanza una excepción que tiene que atrapar el ViewModel (etapa 2);
 * - no guarda nada, así que sin red no hay nada que mostrar (etapa 3).
 */
class IssRepository(private val api: IssApiService) {

    suspend fun fetchPosition(): IssPosition {
        val response = api.getPosition()
        val body = response.body()
        if (!response.isSuccessful || body == null) throw HttpException(response)
        return body.toDomain()
    }
}
