package dev.nicolai.weather.service

import dev.nicolai.weather.model.TemperatureUnit
import dev.nicolai.weather.model.WeatherForecast

interface WeatherService {

    suspend fun getForecast(
        location: String,
        unit: TemperatureUnit,
    ): WeatherForecast
}
