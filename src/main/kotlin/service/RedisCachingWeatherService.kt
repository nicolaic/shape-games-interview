package dev.nicolai.weather.service

import dev.nicolai.weather.model.TemperatureUnit
import dev.nicolai.weather.model.WeatherForecast
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.SetArgs.Builder.exAt
import io.lettuce.core.api.coroutines
import kotlinx.serialization.json.Json
import kotlin.time.Instant

typealias RedisWeatherForecastLoader =
        suspend (location: String, unit: TemperatureUnit) -> Pair<WeatherForecast, Instant>

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisCachingWeatherService(
    private val loader: RedisWeatherForecastLoader,
    redis: RedisClient,
) : WeatherService {
    private val commands = redis.connect().coroutines()

    override suspend fun getForecast(
        location: String,
        unit: TemperatureUnit
    ): WeatherForecast {
        val key = createRedisKey(location, unit)
        val forecast = commands.get(key)

        return if (forecast == null) {
            loader(location, unit).also { (forecast, expiresAt) ->
                val json = Json.encodeToString(forecast)
                commands.set(location, json, exAt(expiresAt.epochSeconds))
            }.first
        } else {
            Json.decodeFromString(forecast)
        }
    }
}

private fun createRedisKey(location: String, unit: TemperatureUnit) = "forecast:$location:$unit"