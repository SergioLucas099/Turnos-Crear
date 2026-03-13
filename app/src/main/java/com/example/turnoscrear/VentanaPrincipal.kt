package com.example.turnoscrear

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.turnoscrear.adapter.ResumenTurnoAdapter
import com.example.turnoscrear.adapter.VerAtraccionAdapter
import com.example.turnoscrear.model.Atraccion
import com.example.turnoscrear.model.CrearTurnosMultiplesRequest
import com.example.turnoscrear.model.TurnoResumenResponse
import com.example.turnoscrear.network.ApiClient
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.post
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class VentanaPrincipal : AppCompatActivity() {

    private lateinit var RevVerAtraccion: RecyclerView
    private lateinit var disminuir: ImageView
    private lateinit var ContadorPersonas: EditText
    private lateinit var agregar: ImageView
    private lateinit var CountryCodePicker: CountryCodePicker
    private lateinit var edtxNumero: TextInputEditText
    private lateinit var btnSubirInfo: Button
    private lateinit var adapter: VerAtraccionAdapter
    private var listaAtracciones = mutableListOf<Atraccion>()
    var contador = 1
    var datoNumeroTelefonico = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ventana_principal)

        RevVerAtraccion = findViewById(R.id.RevVerAtraccion)
        disminuir = findViewById(R.id.disminuir)
        ContadorPersonas = findViewById(R.id.ContadorPersonas)
        agregar = findViewById(R.id.agregar)
        CountryCodePicker = findViewById(R.id.CountryCodePicker)
        edtxNumero = findViewById(R.id.edtxNumero)
        btnSubirInfo = findViewById(R.id.btnSubirInfo)

        CountryCodePicker.registerCarrierNumberEditText(edtxNumero)

        ContadorPersonas.setText("1")

        // Obtener Datos Atracciones
        adapter = VerAtraccionAdapter(
            listaAtracciones,
            { atraccionActualizada -> actualizarAtraccion(atraccionActualizada) }
        )
        RevVerAtraccion.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        RevVerAtraccion.adapter = adapter
        cargarAtracciones()
        conectarWebSocket()

        // Configurar Botones Aumentar Disminuir Personas

        disminuir.setOnClickListener {
            val valorActual = ContadorPersonas.text.toString().toIntOrNull() ?: 1
            contador = valorActual

            if (contador > 1) {
                contador--
            } else {
                contador = 1
            }

            ContadorPersonas.setText(contador.toString())
            ContadorPersonas.setSelection(ContadorPersonas.text.length)
        }

        agregar.setOnClickListener {
            val valorActual = ContadorPersonas.text.toString().toIntOrNull() ?: 1
            contador = valorActual + 1

            ContadorPersonas.setText(contador.toString())
            ContadorPersonas.setSelection(ContadorPersonas.text.length)
        }

        btnSubirInfo.setOnClickListener {

            if (edtxNumero.text.toString().isEmpty()){
                datoNumeroTelefonico = "No Registrado"
            } else {
                datoNumeroTelefonico = edtxNumero.text.toString()
            }

            if (ContadorPersonas.text.isNullOrEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val atraccionesSeleccionadas =
                listaAtracciones.filter { it.seleccionada }

            if (atraccionesSeleccionadas.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos una atracción", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = CrearTurnosMultiplesRequest(
                atraccionesIds = atraccionesSeleccionadas.map { it._id!! },
                telefono = datoNumeroTelefonico,
                cantidadPersonas = ContadorPersonas.text.toString().toInt(),
                fecha = LocalDate.now().toString()
            )

            lifecycleScope.launch {
                try {

                    val respuesta: List<TurnoResumenResponse> =
                        ApiClient.client.post("${ApiClient.BASE_URL}/turnos/preview") {
                            contentType(io.ktor.http.ContentType.Application.Json)
                            setBody(request)
                        }.body()

                    mostrarDialogConfirmacion(respuesta)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@VentanaPrincipal, "Error al obtener preview", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cargarAtracciones() {
        lifecycleScope.launch {
            try {
                val lista: List<Atraccion> =
                    ApiClient.client.get("${ApiClient.BASE_URL}/atracciones")
                        .body()
                adapter.actualizarLista(lista)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun actualizarAtraccion(atraccion: Atraccion){
        lifecycleScope.launch {
            try {
                ApiClient.client.put("${ApiClient.BASE_URL}/atracciones/${atraccion._id}") {
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(atraccion)
                }
                cargarAtracciones()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun conectarWebSocket() {

        lifecycleScope.launch {

            try {
                ApiClient.client.webSocket(
                    method = io.ktor.http.HttpMethod.Get,
                    host = "192.168.0.200",
                    port = 8080,
                    path = "/ws/atracciones"
                ) {
                    //WebSocket conectado a atracciones
                    for (frame in incoming) {
                        if (frame is Frame.Text) {

                            val mensaje = frame.readText()

                            if (mensaje == "ATRACCIONES_UPDATED") {

                                withContext(Dispatchers.Main) {
                                    cargarAtracciones()
                                }
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                println("❌ Error WebSocket: ${e.message}")
                e.printStackTrace()
            }
        }
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mostrarDialogConfirmacion(listaResumen: List<TurnoResumenResponse>) {

        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmar_info, null)

        val recycler = dialogView.findViewById<RecyclerView>(R.id.RevListaDatosAtracciones)
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        recycler.adapter = ResumenTurnoAdapter(listaResumen)

        dialogView.findViewById<TextView>(R.id.txtDatosPersonasTurno).text =
            ContadorPersonas.text.toString()

        dialogView.findViewById<TextView>(R.id.txtDatosTelefonoTurno).text =
            edtxNumero.text.toString()

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton("Cancelar") { d, _ ->
                d.dismiss()
            }
            .setPositiveButton("Aceptar") { d, _ ->
                confirmarCreacionTurnos(listaResumen)
                d.dismiss()
            }
            .create()

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun confirmarCreacionTurnos(listaResumen: List<TurnoResumenResponse>) {

        btnSubirInfo.isEnabled = false

        val atraccionesIds = listaResumen.map { it.atraccionId }

        val request = CrearTurnosMultiplesRequest(
            atraccionesIds = atraccionesIds,
            telefono = edtxNumero.text.toString(),
            cantidadPersonas = ContadorPersonas.text.toString().toInt(),
            fecha = java.time.LocalDate.now().toString()
        )

        lifecycleScope.launch {
            try {

                val response = ApiClient.client.post("${ApiClient.BASE_URL}/turnos/multiple") {
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(request)
                }

                println("STATUS: ${response.status}")

                Toast.makeText(
                    this@VentanaPrincipal,
                    "Turnos creados con éxito",
                    Toast.LENGTH_LONG
                ).show()

                limpiarFormulario()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@VentanaPrincipal,
                    "Error al crear los turnos",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                btnSubirInfo.isEnabled = true
            }
        }
    }

    private fun limpiarFormulario() {

        // Limpiar selección de atracciones
        listaAtracciones.forEach { it.seleccionada = false }
        adapter.notifyDataSetChanged()

        // Reset contador
        ContadorPersonas.setText("1")

        // Limpiar teléfono
        edtxNumero.text?.clear()
    }
}