package com.example.checadorccl

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.checadorccl.api.ApiClient
import com.example.checadorccl.utils.BiometricAuthManager
import com.example.checadorccl.utils.LocationHelper
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.checadorccl.utils.ToastHelper

class JustificationActivity : AppCompatActivity() {

    private lateinit var containerSlots: LinearLayout
    private lateinit var txtFileName: TextView
    private lateinit var imgPreview: ImageView
    private lateinit var editObservaciones: TextInputEditText

    private lateinit var biometricManager: BiometricAuthManager
    private lateinit var locationHelper: LocationHelper

    private var incidenceType: String = ""
    private var incidenceId: Int = 0

    // Guardamos la URI real del archivo seleccionado
    private var archivoSeleccionadoUri: Uri? = null
    // Para guardar temporalmente la foto de cámara
    private var archivoCamaraFile: File? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                imgPreview.setImageBitmap(imageBitmap)
                imgPreview.visibility = View.VISIBLE
                txtFileName.text = "Foto de cámara lista"
                // Guardar bitmap en archivo temporal para poder enviarlo
                archivoCamaraFile = bitmapToFile(imageBitmap)
                archivoSeleccionadoUri = null // Limpiar selección de galería
            }
        }
    }

    private val fileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                archivoSeleccionadoUri = uri
                archivoCamaraFile = null // Limpiar foto de cámara
                txtFileName.text = "Archivo adjunto listo"
                imgPreview.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_justification)

        biometricManager = BiometricAuthManager(this, activity = this)
        locationHelper = LocationHelper(this)

        incidenceType = intent.getStringExtra("INCIDENCE_TYPE") ?: "Omisión"
        val incidenceDate = intent.getStringExtra("INCIDENCE_DATE") ?: ""
        incidenceId = intent.getIntExtra("INCIDENCE_ID", 0)

        val txtTitle = findViewById<TextView>(R.id.txt_incidence_title)
        val txtDetails = findViewById<TextView>(R.id.txt_incidence_details)
        val layoutPayment = findViewById<LinearLayout>(R.id.layout_payment_section)
        containerSlots = findViewById<LinearLayout>(R.id.container_payment_slots)
        txtFileName = findViewById<TextView>(R.id.txt_file_name_justification)
        imgPreview = findViewById<ImageView>(R.id.img_evidence_preview)
        editObservaciones = findViewById(R.id.edit_observation_justification)

        txtTitle.text = "Justificar $incidenceType"
        txtDetails.text = "Incidencia del $incidenceDate"

        val esDeudaTiempo = incidenceType.contains("Retraso", true) || incidenceType.contains("Salida Temprana", true)

        if (esDeudaTiempo) {
            layoutPayment.visibility = View.VISIBLE
            findViewById<Button>(R.id.btn_add_payment_date).setOnClickListener {
                agregarFilaDePago()
            }
        } else {
            layoutPayment.visibility = View.GONE
        }

        findViewById<Button>(R.id.btn_take_photo).setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try { cameraLauncher.launch(takePictureIntent) } catch (e: Exception) { }
        }

        findViewById<Button>(R.id.btn_attach_file).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            val mimeTypes = arrayOf("image/*", "application/pdf")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            fileLauncher.launch(intent)
        }

        findViewById<Button>(R.id.btn_submit_justification).setOnClickListener {
            iniciarProcesoDeEnvio()
        }
    }

    private fun iniciarProcesoDeEnvio() {
        biometricManager.authenticate(
            title = "Confirmar Justificación",
            subtitle = "Verifica tu identidad para enviar"
        ) {
            ToastHelper.show(this, "Identidad verificada. Enviando...")
            locationHelper.getCurrentLocation { lat, long ->
                enviarDatosAlServidor(lat, long)
            }
        }
    }

    private fun enviarDatosAlServidor(lat: Double, long: Double) {
        val prefs = getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("ACCESS_TOKEN", null) ?: return

        // 1. Preparar Textos
        val comentarios = editObservaciones.text.toString()
        val comentariosPart = comentarios.toRequestBody("text/plain".toMediaTypeOrNull())

        // 2. Preparar JSON de Fechas de Pago (Recorriendo la vista visual)
        val listaPagos = mutableListOf<Map<String, String>>()
        for (i in 0 until containerSlots.childCount) {
            val view = containerSlots.getChildAt(i)
            val txtDate = view.findViewById<TextView>(R.id.txt_payment_date).text.toString() // "20/11/2025"
            val txtRange = view.findViewById<TextView>(R.id.txt_payment_time_range).text.toString() // "17:00 - 18:00"

            // Parsear de vuelta para el JSON
            val parts = txtRange.split(" - ")
            if (parts.size >= 2) {
                // Convertir fecha de DD/MM/YYYY a YYYY-MM-DD
                val dateParts = txtDate.split("/")
                val fechaISO = "${dateParts[2]}-${dateParts[1]}-${dateParts[0]}"

                val map = mapOf(
                    "fecha" to fechaISO,
                    "hora_inicio" to parts[0].trim(),
                    "hora_fin" to parts[1].trim()
                )
                listaPagos.add(map)
            }
        }

        val fechasJsonString = Gson().toJson(listaPagos)
        val fechasPart = fechasJsonString.toRequestBody("text/plain".toMediaTypeOrNull())

        // 3. Preparar Archivo
        var bodyArchivo: MultipartBody.Part? = null

        // Opción A: Foto de cámara
        if (archivoCamaraFile != null) {
            val reqFile = archivoCamaraFile!!.asRequestBody("image/jpeg".toMediaTypeOrNull())
            bodyArchivo = MultipartBody.Part.createFormData("archivo", archivoCamaraFile!!.name, reqFile)
        }
        // Opción B: Archivo de galería
        else if (archivoSeleccionadoUri != null) {
            val file = getFileFromUri(archivoSeleccionadoUri!!)
            if (file != null) {
                // Intentar adivinar tipo, o default application/pdf
                val mimeType = contentResolver.getType(archivoSeleccionadoUri!!) ?: "application/pdf"
                val reqFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                bodyArchivo = MultipartBody.Part.createFormData("archivo", file.name, reqFile)
            }
        }

        // 4. Llamada RETROFIT
        lifecycleScope.launch {
            try {
                val response = ApiClient.service.justifyIncidence(
                    token = "Bearer $token",
                    lat = lat.toString(),
                    long = long.toString(),
                    id = incidenceId,
                    comentarios = comentariosPart,
                    fechasPagoJson = if (listaPagos.isNotEmpty()) fechasPart else null,
                    archivo = bodyArchivo
                )

                if (response.isSuccessful) {
                    ToastHelper.show(this@JustificationActivity, "¡Enviado correctamente!")
                    finish() // Cerrar actividad
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e("API_ERROR", "Error ${response.code()}: $errorMsg")
                    ToastHelper.show(this@JustificationActivity, "Error al enviar: ${response.code()}", true)
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Excepción", e)
                ToastHelper.show(this@JustificationActivity, "Error de conexión", true)
            }
        }
    }

    // --- UTILIDADES PARA ARCHIVOS ---

    private fun bitmapToFile(bitmap: Bitmap): File {
        // Crea un archivo temporal en caché
        val file = File(cacheDir, "foto_justificacion_${System.currentTimeMillis()}.jpg")
        val fos = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
        fos.flush()
        fos.close()
        return file
    }

    private fun getFileFromUri(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(cacheDir, "temp_upload_file") // Nombre genérico temporal
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // ... (Funciones agregarFilaDePago, abrirTimePicker, crearVistaSlot IGUAL QUE ANTES) ...
    private fun agregarFilaDePago() {
        val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Fecha de pago").setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            val fechaSeleccionada = format.format(Date(selection))
            abrirTimePicker { horaInicio ->
                abrirTimePicker { horaFin -> crearVistaSlot(fechaSeleccionada, horaInicio, horaFin) }
            }
        }
        datePicker.show(supportFragmentManager, "PAY_DATE")
    }

    private fun abrirTimePicker(onTimeSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            onTimeSelected(String.format("%02d:%02d", hour, minute))
        }
        TimePickerDialog(this, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun crearVistaSlot(fecha: String, inicio: String, fin: String) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.item_payment_slot, containerSlots, false)
        val txtDate = view.findViewById<TextView>(R.id.txt_payment_date)
        val txtTime = view.findViewById<TextView>(R.id.txt_payment_time_range)
        val btnRemove = view.findViewById<ImageButton>(R.id.btn_remove_slot)
        txtDate.text = fecha
        txtTime.text = "$inicio - $fin"
        btnRemove.setOnClickListener { containerSlots.removeView(view) }
        containerSlots.addView(view)
    }
}