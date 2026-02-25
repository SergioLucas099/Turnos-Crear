package com.example.turnoscrear.model

data class Turno (
    val _id: String? = null,
    val atraccionId: String,
    val nombre: String,
    val numeroTurno: String,
    val telefono: String,
    val tiempoEspera: Int = 0,
    val estado: EstadoTurno = EstadoTurno.ESPERA,
    val fecha: String
)