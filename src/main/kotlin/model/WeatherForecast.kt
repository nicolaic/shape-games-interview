package dev.nicolai.weather.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
class WeatherForecast(
    val id: String,
    val city: String,
    val temperatures: Map<Instant, Double>,
)
