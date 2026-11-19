# M3 ISS · ¿Dónde está la ISS?

Una pantalla de la app que consulta en vivo la posición de la Estación Espacial Internacional.
Es una excusa didáctica para tres tópicos del Módulo 3:

| Tópico | Etapas |
|---|---|
| Retrofit y OkHttp: DTO, endpoints, cliente y clasificación de respuestas | [1](etapa-1.md), [2](etapa-2.md) |
| Uso sin conexión: Room como fuente de verdad, caché local y conectividad | [3](etapa-3.md), [4](etapa-4.md) |
| WorkManager: trabajo periódico, restricciones, reintentos y trabajo único | [5](etapa-5.md) |
| Evidencia y conexión con los recordatorios | [6](etapa-6.md), [evidencias](evidencias.md) |

**Acceso:** menú ⋮ (*Ejemplos*) → **¿Dónde está la ISS?**

**API:** [Where the ISS at?](https://wheretheiss.at/w/developer), `GET v1/satellites/25544`, por
HTTPS y sin API key. Es una API comunitaria, no de la NASA. Permite ≈1 solicitud por segundo; la
app nunca consulta más de una vez cada 10 s.

## Estructura

```
app/src/main/java/com/example/resumen_modulos1y2/iss/
  data/remote/   IssPositionDto.kt, IssApiService.kt, RemoteResult.kt
  data/local/    IssPositionEntity.kt, IssPositionDao.kt
  data/          IssRepository.kt, Mappers.kt, ConnectivityObserver.kt
  domain/        IssPosition.kt, EstadoSincronizacion.kt
  work/          IssSyncWorker.kt, IssWorkScheduler.kt
  ui/            IssViewModel.kt, IssScreen.kt, IssUiState.kt
app/src/debug/…/RegistroDeRed.kt     HttpLoggingInterceptor (solo debug)
app/src/release/…/RegistroDeRed.kt   sin registro
```

La consigna pedía el paquete `cl.ipp.recordatorios`. Se usó el paquete real de la app,
`com.example.resumen_modulos1y2`, con la misma estructura debajo de `iss/`
([etapa 1](etapa-1.md#decisiones-respecto-de-la-consigna)).

## Versiones

| Librería | Versión | Nota |
|---|---|---|
| Retrofit + converter-moshi | 3.0.0 | última estable |
| OkHttp + logging-interceptor + mockwebserver | 4.12.0 | existe la 5.5.1, pero Retrofit 3.0.0 se compila contra la 4.12.0 |
| Moshi + moshi-kotlin-codegen (KSP) | 1.15.2 | última estable |
| Room | 2.8.5 | la que ya usaba el proyecto (la consigna sugería 2.8.4) |
| WorkManager (runtime-ktx, testing) | 2.12.0 | última estable |

## Tags

`m3-iss-etapa-1` … `m3-iss-etapa-6`: un commit por etapa, y cada uno compila y se puede mostrar
por sí solo.
