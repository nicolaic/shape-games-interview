package dev.nicolai.weather.routes

import dev.nicolai.weather.model.TemperatureUnit
import dev.nicolai.weather.service.WeatherService
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*

fun Route.byLocation(service: WeatherService) = get("/locations/{location}") {
    val location: String by call.pathParameters
    val forecast = service.getForecast(location, TemperatureUnit.CELSIUS)

    call.respond(forecast)
}
