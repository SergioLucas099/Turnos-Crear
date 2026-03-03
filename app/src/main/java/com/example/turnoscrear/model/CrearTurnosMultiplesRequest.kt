package com.example.turnoscrear.model

import kotlinx.serialization.Serializable

@Serializable
data class CrearTurnosMultiplesRequest(
    val atraccionesIds: List<String>,
    val telefono: String,
    val cantidadPersonas: Int,
    val fecha: String
)