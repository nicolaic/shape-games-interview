package dev.nicolai.weather.routes

import dev.nicolai.weather.model.TemperatureUnit
import dev.nicolai.weather.model.WeatherForecast
import dev.nicolai.weather.service.WeatherService
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

fun Route.summary(service: WeatherService) = get("/summary") {
    val unit = TemperatureUnit.valueOf(call.queryParameters.getOrFail("unit").uppercase())
    val temperature: Double by call.queryParameters
    val locations: List<String> by call.queryParameters

    val forecasts = coroutineScope {
        locations.map { location ->
            async { service.getForecast(location, unit) }
        }.awaitAll()
    }

    val filteredForecasts = forecasts
        .filter { it.doesNext24HoursExceed(temperature) }
        .associateBy(WeatherForecast::id)

    call.respond(filteredForecasts)
}

private fun WeatherForecast.doesNext24HoursExceed(temperatureLimit: Double): Boolean {
    val nowPlus24Hours = Clock.System.now() + 1.days

    return temperatures.any { (time, temperature) ->
        time <= nowPlus24Hours && temperature > temperatureLimit
    }
}
