package com.example.resumen_modulos1y2.iss.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Identificador NORAD de la Estación Espacial Internacional. */
const val ISS_NORAD_ID = 25544

/**
 * Los endpoints se DESCRIBEN con una interfaz y Retrofit escribe la implementación.
 *
 * - `@GET("v1/satellites/{id}")`: ruta relativa a la URL base, que debe terminar en "/".
 * - `@Path("id")`: reemplaza {id} en la ruta.
 * - `@Query("units")`: agrega ?units=kilometers al final.
 * - `Response<T>`: entrega el código HTTP además del cuerpo. Si el tipo de retorno fuera solo
 *   `IssPositionDto`, un 404 o un 429 serían una excepción; con Response se pueden distinguir
 *   (etapa 2).
 * - `suspend`: Retrofit hace la llamada fuera del hilo principal.
 *
 * Límite de la API: ≈1 solicitud por segundo. La app nunca consulta más de una vez cada 10 s.
 */
interface IssApiService {

    @GET("v1/satellites/{id}")
    suspend fun getPosition(
        @Path("id") id: Int = ISS_NORAD_ID,
        @Query("units") units: String = "kilometers"
    ): Response<IssPositionDto>
}
