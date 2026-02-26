@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.nicolai.weather.service

import dev.nicolai.weather.model.TemperatureUnit
import dev.nicolai.weather.model.WeatherForecast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.TimeMark

typealias WeatherForecastLoader = suspend (location: String, unit: TemperatureUnit) -> Pair<WeatherForecast, TimeMark>

class MemoryCachingWeatherService(
    private val scope: CoroutineScope,
    private val loader: WeatherForecastLoader,
) : WeatherService {
    private val cache = ConcurrentHashMap<WeatherCacheKey, Deferred<CacheEntry<WeatherForecast>>>()

    override suspend fun getForecast(
        location: String, unit: TemperatureUnit
    ): WeatherForecast = cache.compute(WeatherCacheKey(location, unit)) { key, value ->
        if (value != null && value.canReuseCachedValue()) return@compute value

        scope.async {
            // TODO: We could implement some retry logic here
            val (value, expiresAt) = loader(key.location, key.unit)
            CacheEntry(value, expiresAt)
        }
    }!!.await().value
}

private fun Deferred<CacheEntry<WeatherForecast>>.canReuseCachedValue(): Boolean =
    this.isCompleted.not()
            || (getCompletionExceptionOrNull() == null
            && getCompleted().expiresAt.hasNotPassedNow())

private data class WeatherCacheKey(val location: String, val unit: TemperatureUnit)

private class CacheEntry<T>(
    val value: T,
    val expiresAt: TimeMark,
)
