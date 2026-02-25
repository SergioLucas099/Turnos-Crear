package com.example.turnoscrear

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.turnoscrear.adapter.VerAtraccionAdapter
import com.example.turnoscrear.model.Atraccion
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
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

                    println("✅ WebSocket conectado a atracciones")

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
}