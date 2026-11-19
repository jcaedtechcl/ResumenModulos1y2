package com.example.resumen_modulos1y2.iss.data

import com.example.resumen_modulos1y2.iss.data.remote.IssApiService
import com.example.resumen_modulos1y2.iss.data.remote.RemoteResult
import com.example.resumen_modulos1y2.iss.data.remote.map
import com.example.resumen_modulos1y2.iss.data.remote.safeApiCall
import com.example.resumen_modulos1y2.iss.domain.IssPosition

/**
 * ETAPA 2: el repositorio ya no lanza excepciones. Cada resultado de red se clasifica aquí, en
 * la capa de datos, y sale como RemoteResult con el DTO ya traducido al dominio.
 *
 * Pendiente para la etapa 3: no guarda nada, así que sin red no hay nada que mostrar.
 */
class IssRepository(private val api: IssApiService) {

    suspend fun fetchPosition(): RemoteResult<IssPosition> =
        safeApiCall { api.getPosition() }.map { it.toDomain() }
}
