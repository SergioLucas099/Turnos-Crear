package com.example.turnoscrear.model

import kotlinx.serialization.Serializable

@Serializable
data class Turno (
    val _id: String? = null,
    val atraccionId: String,
    val nombreAtraccion: String,
    val numeroTurno: String,
    val numeroPersonas: Int,
    val telefono: String,
    val tiempoEspera: Int = 0,
    val estado: EstadoTurno = EstadoTurno.ESPERA,
    val fecha: String
)