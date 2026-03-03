package com.example.turnoscrear.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.turnoscrear.R
import com.example.turnoscrear.model.Atraccion

class VerAtraccionAdapter (
    private val lista: MutableList<Atraccion>,
    private val onEditar: (Atraccion) -> Unit
) : RecyclerView.Adapter<VerAtraccionAdapter.VerAtraccionViewHolder>() {

    inner class VerAtraccionViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
            val nombre: TextView = itemView.findViewById(R.id.nombreAtraccion)
            val SeleccionarAtraccion: ConstraintLayout = itemView.findViewById(R.id.ContenidoAtraccion)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) :
            VerAtraccionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ver_atraccion_item, parent, false)
        return VerAtraccionViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerAtraccionViewHolder, position: Int) {

        val atraccion = lista[position]

        holder.nombre.text = atraccion.nombre

        // Cambiar color según selección
        if (atraccion.seleccionada) {
            holder.SeleccionarAtraccion.setBackgroundColor(
                holder.itemView.context.getColor(R.color.green_dark)
            )
        } else {
            holder.SeleccionarAtraccion.setBackgroundColor(
                holder.itemView.context.getColor(R.color.green)
            )
        }

        holder.SeleccionarAtraccion.setOnClickListener {
            atraccion.seleccionada = !atraccion.seleccionada
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<Atraccion>) {
        lista.clear()
        lista.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}