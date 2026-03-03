package com.example.turnoscrear.model

import kotlinx.serialization.Serializable

@Serializable
data class TurnoResumenResponse(
    val atraccionId: String,
    val nombreAtraccion: String,
    val numeroTurno: String,
    val duracionSegundos: Int,
    val tiempoEspera: Int,
    val turnoActualAnterior: String
)