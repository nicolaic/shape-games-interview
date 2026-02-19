package dev.nicolai.weather

import dev.nicolai.weather.routes.byLocation
import dev.nicolai.weather.routes.summary
import dev.nicolai.weather.service.OpenWeatherMapWeatherService
import dev.nicolai.weather.service.WeatherService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.di.*
import io.ktor.server.routing.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDependencies()
    configureSerialization()
    configureRouting()
}

fun Application.configureRouting() {
    // This could be injected inside the routes with application.dependencies
    val service: WeatherService by dependencies

    routing {
        route("/weather") {
            summary(service)
            byLocation(service)
        }
    }
}


fun Application.configureSerialization() {
    install(ServerContentNegotiation) {
        json()
    }
}

fun Application.configureDependencies() {
    dependencies {
        provide<HttpClient> {
            HttpClient(CIO) {
                install(ClientContentNegotiation) {
                    json()
                }
            }
        }

        provide<WeatherService>(OpenWeatherMapWeatherService::class)
    }
}
