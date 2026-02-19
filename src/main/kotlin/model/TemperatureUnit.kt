package dev.nicolai.weather.model

import kotlinx.serialization.SerialName

enum class TemperatureUnit {
    @SerialName("celsius") CELSIUS,
    @SerialName("fahrenheit") FAHRENHEIT,
}
