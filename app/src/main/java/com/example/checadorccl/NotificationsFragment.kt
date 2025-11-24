package com.example.checadorccl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.checadorccl.api.ApiClient
import com.example.checadorccl.api.models.NotificationResponse
import com.example.checadorccl.api.models.IncidenciaResponse
import com.example.checadorccl.api.models.EventoResponse
import com.example.checadorccl.utils.ShakeDetector
import com.google.gson.Gson
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var txtNoNotifs: TextView

    private var sensorManager: android.hardware.SensorManager? = null
    private var accelerometer: android.hardware.Sensor? = null
    private var shakeDetector: ShakeDetector? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recycler_notifications)
        txtNoNotifs = view.findViewById(R.id.txt_no_notifs)
        recycler.layoutManager = LinearLayoutManager(context)

        fetchNotifications()

        // Sensor Shake
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        accelerometer = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        shakeDetector = ShakeDetector {
            Toast.makeText(context, "Actualizando...", Toast.LENGTH_SHORT).show()
            fetchNotifications()
        }
    }

    private fun fetchNotifications() {
        val prefs = requireContext().getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("ACCESS_TOKEN", null) ?: return

        lifecycleScope.launch {
            try {
                val response = ApiClient.service.getNotifications("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val lista = response.body()!!

                    if (lista.isEmpty()) {
                        txtNoNotifs.visibility = View.VISIBLE
                        recycler.visibility = View.GONE
                    } else {
                        txtNoNotifs.visibility = View.GONE
                        recycler.visibility = View.VISIBLE

                        recycler.adapter = NotificationsAdapter(lista) { notif ->
                            abrirDetalle(notif)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NOTIF_ERROR", "Error cargando notificaciones", e)
            }
        }
    }

    // ESTA ES LA ÚNICA VERSIÓN DE ABRIR DETALLE (LA CORRECTA)
    private fun abrirDetalle(notif: NotificationResponse) {
        val prefs = requireContext().getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("ACCESS_TOKEN", null) ?: return

        Toast.makeText(context, "Cargando detalles...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // Llamamos al endpoint de detalle
                val response = ApiClient.service.getNotificationDetail("Bearer $token", notif.id)

                if (response.isSuccessful && response.body() != null) {
                    val detalle = response.body()!!
                    val gson = Gson()

                    if (detalle.tipoObjeto == "INCIDENCIA" && detalle.data != null) {
                        // Convertir el JSON genérico a IncidenciaResponse
                        val incidencia = gson.fromJson(detalle.data, IncidenciaResponse::class.java)

                        val intent = Intent(context, IncidenceDetailActivity::class.java)
                        intent.putExtra("INCIDENCIA_OBJ", incidencia)
                        startActivity(intent)

                    } else if (detalle.tipoObjeto == "EVENTO" && detalle.data != null) {
                        // Convertir el JSON genérico a EventoResponse
                        val evento = gson.fromJson(detalle.data, EventoResponse::class.java)

                        val intent = Intent(context, EventDetailsActivity::class.java)
                        intent.putExtra("EVENTO_OBJ", evento)
                        intent.putExtra("EVENT_COLOR", "#2196F3")
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "Detalle no disponible", Toast.LENGTH_SHORT).show()
                    }

                    // Recargar lista para que se marque como leída
                    fetchNotifications()
                }
            } catch (e: Exception) {
                Log.e("NOTIF_CLICK", "Error", e)
                Toast.makeText(context, "Error al abrir notificación", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager?.registerListener(shakeDetector, it, android.hardware.SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeDetector)
    }
}