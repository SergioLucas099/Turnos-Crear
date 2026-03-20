package com.example.turnoscrear.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.turnoscrear.R
import com.example.turnoscrear.model.TurnoResumenResponse
import com.example.turnoscrear.model.Turnos
import com.example.turnoscrear.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType

class TurnosAdapter (
    private val lista: MutableList<Turnos>,
    private val onEditar: (Turnos) -> Unit
) : RecyclerView.Adapter<TurnosAdapter.TurnosAdapterViewHolder>(){

    inner class TurnosAdapterViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val tiempoTurno: TextView =
            itemView.findViewById(R.id.tiempoTurno)
        val nombreAtraccionSelect: TextView =
            itemView.findViewById(R.id.nombreAtraccionSelect)
        val personasTurno: TextView =
            itemView.findViewById(R.id.personasTurno)
        val TurnoAsignado: TextView =
            itemView.findViewById(R.id.TurnoAsignado)
        val ImprimirTurno: ImageView =
            itemView.findViewById(R.id.ImprimirTurno)
        val SeleccionarTurno: LinearLayout =
            itemView.findViewById(R.id.SeleccionarTurno)
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TurnosAdapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.turnos_item, parent, false)

        return TurnosAdapterViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(
        holder: TurnosAdapterViewHolder,
        position: Int) {

        val turno = lista[position]

        holder.tiempoTurno.text = formatearTiempo(turno.duracion)
        holder.nombreAtraccionSelect.text = turno.nombreAtraccion
        holder.personasTurno.text = turno.numeroPersonas.toString()
        holder.TurnoAsignado.text = turno.numeroTurno
        holder.ImprimirTurno.setOnClickListener {

            val turnoSeleccionado = lista[position]

            CoroutineScope(Dispatchers.IO).launch {
                try {

                    val listaImpresion = listOf(
                        TurnoResumenResponse(
                            atraccionId = turnoSeleccionado.atraccionId,
                            nombreAtraccion = turnoSeleccionado.nombreAtraccion,
                            numeroTurno = turnoSeleccionado.numeroTurno,
                            duracionSegundos = turnoSeleccionado.duracion,
                            tiempoEspera = turnoSeleccionado.tiempoEspera,
                            turnoActualAnterior = "0000"
                        )
                    )

                    ApiClient.client.post("${ApiClient.BASE_URL}/turnos/imprimir") {
                        contentType(io.ktor.http.ContentType.Application.Json)
                        setBody(listaImpresion)
                    }

                    Toast.makeText(
                        holder.itemView.context,
                        "Reimprimiendo ticket...",
                        Toast.LENGTH_SHORT
                    ).show()

                } catch (e: Exception) {
                    Toast.makeText(
                        holder.itemView.context,
                        "Error al imprimir",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        holder.SeleccionarTurno.setOnClickListener {
            val dialogView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.dialog_validar_info, null)

            val txtDatosAtraccionTurno = dialogView.findViewById<TextView>(R.id.txtDatosAtraccionTurno)
            txtDatosAtraccionTurno.text = turno.nombreAtraccion

            val txtDatosNumeroTurno = dialogView.findViewById<TextView>(R.id.txtDatosNumeroTurno)
            txtDatosNumeroTurno.text = turno.numeroTurno

            val txtDatosPersonasTurno = dialogView.findViewById<TextView>(R.id.txtDatosPersonasTurno)
            txtDatosPersonasTurno.text = turno.numeroPersonas.toString()

            val txtDatosTelefonoTurno = dialogView.findViewById<TextView>(R.id.txtDatosTelefonoTurno)
            txtDatosTelefonoTurno.text = turno.telefono
            if (turno.telefono.isEmpty()) {
                txtDatosTelefonoTurno.text = "No registrado"
            }

            val txtDatosTiempoTurno = dialogView.findViewById<TextView>(R.id.txtDatosTiempoTurno)
            txtDatosTiempoTurno.text = formatearTiempo(turno.duracion)

            val txtDatosTiempoEsperaTurno = dialogView.findViewById<TextView>(R.id.txtDatosTiempoEsperaTurno)
            txtDatosTiempoEsperaTurno.text = formatearTiempo(turno.tiempoEspera)

            val txtDatosEstadoTurno = dialogView.findViewById<TextView>(R.id.txtDatosEstadoTurno)
            txtDatosEstadoTurno.text = turno.estado

            val txtDatosFechaTurno = dialogView.findViewById<TextView>(R.id.txtDatosFechaTurno)
            val fecha = LocalDateTime.parse(turno.fecha)

            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")

            txtDatosFechaTurno.text = fecha
                .format(formatter)
                .replace("AM","a.m")
                .replace("PM","p.m")

            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setView(dialogView)
                .setPositiveButton("Aceptar") { dialog, _ ->
                    // acción si el usuario confirma
                    dialog.dismiss()
                }

            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<Turnos>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}

private fun formatearTiempo(segundos: Int): String {

    val horas = segundos / 3600
    val minutos = (segundos % 3600) / 60
    val seg = segundos % 60

    return when {
        horas > 0 -> String.format("%02d:%02d hr", horas, minutos)
        minutos > 0 -> String.format("%02d:%02d min", minutos, seg)
        else -> String.format("00:%02d seg", seg)
    }
}