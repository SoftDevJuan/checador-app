package com.example.checadorccl

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.checadorccl.api.ApiClient
import com.example.checadorccl.models.AttendanceRecord
import com.example.checadorccl.models.IncidenceType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.checadorccl.utils.ToastHelper

class AttendanceFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: AttendanceAdapter
    private lateinit var txtName: TextView
    private lateinit var txtPosition: TextView
    private lateinit var btnCurrentFortnight: Button
    private lateinit var btnPreviousFortnight: Button // Nuevo botón
    private var sensorManager: android.hardware.SensorManager? = null
    private var accelerometer: android.hardware.Sensor? = null
    private var shakeDetector: com.example.checadorccl.utils.ShakeDetector? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler = view.findViewById(R.id.recycler_attendance_records)
        txtName = view.findViewById(R.id.txt_employee_name)
        txtPosition = view.findViewById(R.id.txt_employee_position)
        btnCurrentFortnight = view.findViewById(R.id.btn_quincena_actual)
        btnPreviousFortnight = view.findViewById(R.id.btn_quincena_anterior) // Vincular ID del XML



        recycler.layoutManager = LinearLayoutManager(context)

        loadUserProfile()

        // Cargar por defecto la actual
        fetchAttendanceData()

        // 1. BOTÓN QUINCENA ACTUAL (Sin fechas, el server calcula)
        btnCurrentFortnight.setOnClickListener {
            fetchAttendanceData(startDate = null, endDate = null)
            ToastHelper.show(requireContext(), "Cargando quincena actual...")
        }

        // 2. BOTÓN QUINCENA ANTERIOR (Calculamos fechas aquí)
        btnPreviousFortnight.setOnClickListener {
            cargarQuincenaAnterior()
        }

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        accelerometer = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
        shakeDetector = com.example.checadorccl.utils.ShakeDetector {
            // ACCIÓN AL SACUDIR: Recargar datos
            ToastHelper.show(requireContext(), "🔄 Actualizando registros...")
            fetchAttendanceData()
        }


    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(shakeDetector, it, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(shakeDetector)
    }

    private fun cargarQuincenaAnterior() {
        val hoy = LocalDate.now()
        val dia = hoy.dayOfMonth

        var inicio: LocalDate
        var fin: LocalDate

        if (dia > 15) {
            // Estamos en la 2da quincena -> La anterior es la 1ra del MISMO mes
            inicio = LocalDate.of(hoy.year, hoy.month, 1)
            fin = LocalDate.of(hoy.year, hoy.month, 15)
        } else {
            // Estamos en la 1ra quincena -> La anterior es la 2da del MES PASADO
            val mesPasado = hoy.minusMonths(1)
            inicio = LocalDate.of(mesPasado.year, mesPasado.month, 16)
            // El último día del mes pasado (28, 29, 30 o 31)
            fin = mesPasado.withDayOfMonth(mesPasado.lengthOfMonth())
        }

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val inicioStr = inicio.format(formatter)
        val finStr = fin.format(formatter)

        ToastHelper.show(requireContext(), "Cargando del $inicioStr al $finStr")

        // Llamamos a la API con las fechas calculadas
        fetchAttendanceData(inicioStr, finStr)
    }

    // Modificamos para aceptar fechas opcionales
    private fun fetchAttendanceData(startDate: String? = null, endDate: String? = null) {
        val prefs = requireContext().getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("ACCESS_TOKEN", null)

        if (token == null) {
            ToastHelper.show(requireContext(), "Sesión expirada.",true)
            return
        }

        // Deshabilitar botones para evitar doble click
        btnCurrentFortnight.isEnabled = false
        btnPreviousFortnight.isEnabled = false

        lifecycleScope.launch {
            try {
                // Pasamos las fechas (o null) a la API
                val response = ApiClient.service.getAttendanceRecords("Bearer $token", startDate, endDate)

                if (response.isSuccessful && response.body() != null) {
                    val listaApi = response.body()!!

                    val listaVisual = listaApi.map { item ->
                        AttendanceRecord(
                            id = item.id,
                            fecha = formatearFecha(item.fecha),
                            entrada = item.entrada ?: "--:--",
                            salida = item.salida ?: "--:--",
                            horasTotales = item.totalHoras ?: "0h",
                            estadoResumen = item.estado ?: "N/A",
                            incidencias = if (item.tipo != null && item.tipo != "Asistencia")
                                listOf(IncidenceType(item.id, item.tipo, obtenerColor(item.tipo)))
                            else emptyList(),
                            requiereJustificacion = item.requiereJustificacion
                        )
                    }

                    adapter = AttendanceAdapter(listaVisual) { record, actionType ->
                        if (actionType == "SOLICITUD") {
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.content_frame, PermitRequestFragment())
                                .addToBackStack(null)
                                .commit()
                        } else if (actionType == "JUSTIFICACION") {
                            val intent = Intent(context, JustificationActivity::class.java)
                            intent.putExtra("INCIDENCE_ID", record.id)
                            intent.putExtra("INCIDENCE_DATE", record.fecha)
                            val tipoPrincipal = if (record.incidencias.isNotEmpty()) record.incidencias[0].nombre else "Incidencia"
                            intent.putExtra("INCIDENCE_TYPE", tipoPrincipal)
                            startActivity(intent)
                        }
                    }

                    recycler.adapter = adapter

                    if (listaVisual.isEmpty()) {
                        ToastHelper.show(requireContext(), "No se encontraron registros en estas fechas.",true)
                    }

                } else {
                    ToastHelper.show(requireContext(), "Error del servidor: ${response.code()}",true)
                }
            } catch (e: Exception) {
                ToastHelper.show(requireContext(), "Error de conexión.",true)
                e.printStackTrace()
            } finally {
                btnCurrentFortnight.isEnabled = true
                btnPreviousFortnight.isEnabled = true
            }
        }
    }

    // ... (loadUserProfile, formatearFecha, obtenerColor, configurarIconoDefault IGUAL QUE ANTES) ...

    private fun loadUserProfile() {
        val prefs = requireContext().getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        val nombre = prefs.getString("USER_NAME", "Empleado")
        val puesto = prefs.getString("USER_POSITION", "General")
        val fotoBase64 = prefs.getString("USER_PHOTO", null)

        txtName.text = "$nombre"
        txtPosition.text = puesto

        val imgProfile = view?.findViewById<ImageView>(R.id.img_profile_attendance)
        if (imgProfile != null) {
            imgProfile.clearColorFilter()
            if (!fotoBase64.isNullOrEmpty()) {
                try {
                    val pureBase64 = if (fotoBase64.contains(",")) fotoBase64.substringAfter(",") else fotoBase64
                    val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                    val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (decodedBitmap != null) {
                        imgProfile.setImageBitmap(decodedBitmap)
                        imgProfile.setPadding(0,0,0,0)
                    }
                } catch (e: Exception) {
                    configurarIconoDefault(imgProfile)
                }
            } else {
                configurarIconoDefault(imgProfile)
            }
        }
    }

    private fun configurarIconoDefault(imageView: ImageView) {
        imageView.setImageResource(R.drawable.ic_person)
        imageView.setPadding(15, 15, 15, 15)
        imageView.setColorFilter(android.graphics.Color.GRAY)
    }

    private fun formatearFecha(fechaStr: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE, dd MMM", Locale("es", "MX"))
            val date = inputFormat.parse(fechaStr)
            return outputFormat.format(date!!).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } catch (e: Exception) {
            return fechaStr
        }
    }

    private fun obtenerColor(tipo: String): String {
        return when {
            tipo.contains("Retraso", true) -> "#FF9800"
            tipo.contains("Falta", true) -> "#F44336"
            tipo.contains("Omisión", true) -> "#9C27B0"
            else -> "#607D8B"
        }
    }
}