package com.example.turnoscrear

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.turnoscrear.adapter.ResumenTurnoAdapter
import com.example.turnoscrear.adapter.TurnosAdapter
import com.example.turnoscrear.adapter.VerAtraccionAdapter
import com.example.turnoscrear.model.Atraccion
import com.example.turnoscrear.model.CrearTurnosMultiplesRequest
import com.example.turnoscrear.model.TurnoResumenResponse
import com.example.turnoscrear.model.Turnos
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class VentanaPrincipal : AppCompatActivity() {

    private lateinit var RevVerAtraccion: RecyclerView
    private lateinit var disminuir: ImageView
    private lateinit var ContadorPersonas: EditText
    private lateinit var agregar: ImageView
    private lateinit var AvisoSinTurnosImprimir: LinearLayout
    private lateinit var VerTurnos: CoordinatorLayout
    private lateinit var CountryCodePicker: CountryCodePicker
    private lateinit var edtxNumero: TextInputEditText
    private lateinit var btnSubirInfo: Button
    private lateinit var VerHistorialTurnos: Button
    private lateinit var OcultarHistorialTurnos: Button
    private lateinit var BuscadorTurnoImprimir: SearchView
    private lateinit var RevListaTurnosImprimir: RecyclerView
    private lateinit var adapter: VerAtraccionAdapter
    private var listaAtracciones = mutableListOf<Atraccion>()
    private lateinit var adapterTurnos: TurnosAdapter
    private val listaTurnosOriginal = mutableListOf<Turnos>()
    var contador = 1
    var datoNumeroTelefonico = ""
    var nombreAtraccion = ""

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
        VerHistorialTurnos = findViewById(R.id.VerHistorialTurnos)
        OcultarHistorialTurnos = findViewById(R.id.OcultarHistorialTurnos)
        BuscadorTurnoImprimir = findViewById(R.id.BuscadorTurnoImprimir)
        RevListaTurnosImprimir = findViewById(R.id.RevListaTurnosImprimir)
        AvisoSinTurnosImprimir = findViewById(R.id.AvisoSinTurnosImprimir)
        VerTurnos = findViewById(R.id.VerTurnos)

        CountryCodePicker.registerCarrierNumberEditText(edtxNumero)

        ContadorPersonas.setText("1")

        // Obtener Datos Atracciones
        adapter = VerAtraccionAdapter(
            listaAtracciones,
            { atraccionActualizada -> actualizarAtraccion(atraccionActualizada) }
        )
        RevVerAtraccion.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        RevVerAtraccion.adapter = adapter

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
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")

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
                fecha = LocalDateTime.now().format(formatter)
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

        VerHistorialTurnos.setOnClickListener {
            VerHistorialTurnos.visibility = View.GONE
            OcultarHistorialTurnos.visibility = View.VISIBLE
            VerTurnos.visibility = View.VISIBLE
            BuscadorTurnoImprimir.visibility = View.VISIBLE
        }

        OcultarHistorialTurnos.setOnClickListener {
            VerHistorialTurnos.visibility = View.VISIBLE
            OcultarHistorialTurnos.visibility = View.GONE
            VerTurnos.visibility = View.GONE
            BuscadorTurnoImprimir.visibility = View.GONE
        }

        adapterTurnos = TurnosAdapter(
            mutableListOf(),
            { turno -> actualizarTurnos(turno) }
        )

        RevListaTurnosImprimir.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

        RevListaTurnosImprimir.adapter = adapterTurnos

        BuscadorTurnoImprimir.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarTurnos(newText ?: "")
                return true
            }
        })
        cargarAtracciones()

        cargarTurnos()

        conectarWebSocket()

        conectarWebSocketTurnos()
    }

    private fun filtrarTurnos(texto: String) {

        val textoLower = texto.lowercase()

        if (textoLower.isEmpty()) {
            adapterTurnos.actualizarLista(listaTurnosOriginal)
            return
        }

        val filtrados = listaTurnosOriginal.filter {

            it.numeroTurno.contains(textoLower, true) ||
                    it.telefono.replace(" ", "").contains(textoLower)
        }

        adapterTurnos.actualizarLista(filtrados)
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

    private fun conectarWebSocketTurnos() {
        lifecycleScope.launch {
            try {
                ApiClient.client.webSocket(
                    method = io.ktor.http.HttpMethod.Get,
                    host = "192.168.0.200",
                    port = 8080,
                    path = "/ws/turnos"
                ) {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {

                            val mensaje = frame.readText()

                            if (mensaje == "TURNOS_UPDATED") {

                                withContext(Dispatchers.Main) {
                                    cargarTurnos()
                                }
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                println("❌ Error WebSocket TURNOS: ${e.message}")
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
            }.setNeutralButton("Imprimir"){ d, _ ->
                confirmarCreacionTurnos(listaResumen)

                lifecycleScope.launch {
                    try {
                        ApiClient.client.post("${ApiClient.BASE_URL}/turnos/imprimir") {
                            contentType(io.ktor.http.ContentType.Application.Json)
                            setBody(listaResumen)
                        }

                        Toast.makeText(this@VentanaPrincipal, "Imprimiendo Ticket...", Toast.LENGTH_SHORT).show()

                    } catch (e: Exception) {
                        Toast.makeText(this@VentanaPrincipal, "Error al imprimir", Toast.LENGTH_SHORT).show()
                    }
                }

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
            fecha = java.time.LocalDateTime.now().toString()
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

                cargarTurnos()

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

    private fun cargarTurnos() {
        lifecycleScope.launch {
            try {
                val lista: List<Turnos> =
                    ApiClient.client
                        .get("${ApiClient.BASE_URL}/turnos")
                        .body()

                val listaFiltrada = if (nombreAtraccion.isEmpty()) {
                    lista
                } else {
                    lista.filter { it.nombreAtraccion == nombreAtraccion }
                }

                val listaCancelados = listaFiltrada.filter { it.estado == "ESPERA" }

                listaTurnosOriginal.clear()
                listaTurnosOriginal.addAll(listaCancelados)

                adapterTurnos.actualizarLista(listaCancelados)

                AvisoSinTurnosImprimir.visibility =
                    if (listaCancelados.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                Log.e("ERROR_TURNOS", e.message ?: "Error desconocido")
            }
        }
    }

    private fun actualizarTurnos(turnos: Turnos){
        lifecycleScope.launch {
            try {
                ApiClient.client.put("${ApiClient.BASE_URL}/turnos/${turnos._id}") {
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(turnos)
                }
                cargarTurnos()
            } catch (e: Exception) {
                e.printStackTrace()
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