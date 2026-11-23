package com.example.ecolife.network

data class AirPollutionResponse(
    val list: List<AirPollutionItem>
)

data class AirPollutionItem(
    val main: AirMain,
    val components: AirComponents
)

data class AirMain(
    val aqi: Int
)

data class AirComponents(
    val pm2_5: Double,
    val pm10: Double,
    val o3: Double,
    val no2: Double
)
