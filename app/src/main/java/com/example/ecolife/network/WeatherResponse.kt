package com.example.ecolife.network

data class WeatherResponse(
    val name: String,
    val main: WeatherMain,
    val weather: List<WeatherCondition>
)

data class WeatherMain(
    val temp: Double,
    val humidity: Int
)

data class WeatherCondition(
    val main: String,
    val description: String,
    val icon: String
)
