@file:OptIn(ExperimentalSerializationApi::class)

package dev.nicolai.weather.service

import dev.nicolai.weather.model.TemperatureUnit
import dev.nicolai.weather.model.TemperatureUnit.CELSIUS
import dev.nicolai.weather.model.TemperatureUnit.FAHRENHEIT
import dev.nicolai.weather.model.WeatherForecast
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.plugins.di.annotations.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.JsonNames
import kotlin.time.Instant

// We use the version 2.5 API, because it accepts city ids
val OPEN_WEATHER_MAP_API_BASE_URL = Url("https://api.openweathermap.org/data/2.5")
val OPEN_WEATHER_MAP_API_FORECAST_URL = URLBuilder(OPEN_WEATHER_MAP_API_BASE_URL)
    .appendPathSegments("forecast").build()

class OpenWeatherMapWeatherService(
    @Property("openWeatherMap.apiKey")
    private val apiKey: String,
    @Property("openWeatherMap.concurrency")
    private val concurrency: Int = 10,
    private val client: HttpClient,
) : WeatherService {
    private val semaphore = Semaphore(concurrency)

    override suspend fun getForecast(
        location: String,
        unit: TemperatureUnit,
    ): WeatherForecast {
        val forecast = semaphore.withPermit {
            client.get(OPEN_WEATHER_MAP_API_FORECAST_URL) {
                accept(ContentType.Application.Json)

                parameter("appid", apiKey)
                parameter("units", mapToOpenWeatherMapUnits(unit))
                parameter("id", location)
            }.body<ForecastResponse>()
        }

        return forecast.toDomainModel()
    }
}

private fun mapToOpenWeatherMapUnits(unit: TemperatureUnit) = when (unit) {
    // We don't need to support kelvin temperatures
    // KELVIN -> "standard"
    CELSIUS -> "metric"
    FAHRENHEIT -> "imperial"
}

private fun ForecastResponse.toDomainModel() = WeatherForecast(
    city.id,
    city.name,
    list.associate {
        Instant.fromEpochSeconds(it.timestamp) to it.main.temp
    }
)

@Serializable
@JsonIgnoreUnknownKeys
private class ForecastResponse(
    val city: CityData,
    val list: List<ForecastData>,
)

@Serializable
@JsonIgnoreUnknownKeys
private class CityData(
    val id: String,
    val name: String,
)

@Serializable
@JsonIgnoreUnknownKeys
private class ForecastData(
    @JsonNames("dt")
    val timestamp: Long,
    val main: ForecastTemperatures,
)

@Serializable
@JsonIgnoreUnknownKeys
private class ForecastTemperatures(
    val temp: Double,
)