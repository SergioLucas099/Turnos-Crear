package com.example.turnoscrear.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.turnoscrear.R
import com.example.turnoscrear.model.TurnoResumenResponse

class ResumenTurnoAdapter(
    private val lista: List<TurnoResumenResponse>
) : RecyclerView.Adapter<ResumenTurnoAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.txtNombreAtraccion)
        val turno: TextView = itemView.findViewById(R.id.txtNumeroTurno)
        val duracion: TextView = itemView.findViewById(R.id.txtDuracion)
        val espera: TextView = itemView.findViewById(R.id.txtTiempoEspera)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resumen_turno, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        holder.nombre.text = "Atracción: ${item.nombreAtraccion}"
        holder.turno.text = "Turno: ${item.numeroTurno}"
        holder.duracion.text = "Duración: ${formatearTiempo(item.duracionSegundos)}"
        holder.espera.text = "Tiempo Espera: ${formatearTiempo(item.tiempoEspera)}"
    }

    private fun formatearTiempo(segundos: Int): String {

        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60
        val segundosRestantes = segundos % 60

        return if (horas > 0) {
            String.format("%02d:%02d:%02d h", horas, minutos, segundosRestantes)
        } else {
            String.format("%02d:%02d min", minutos, segundosRestantes)
        }
    }
}