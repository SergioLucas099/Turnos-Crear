package com.example.turnoscrear.model

import kotlinx.serialization.Serializable

@Serializable
enum class EstadoTurno {
    ESPERA,
    APROBADO,
    LLAMADO,
    FINALIZADO,
    CANCELADO,
}