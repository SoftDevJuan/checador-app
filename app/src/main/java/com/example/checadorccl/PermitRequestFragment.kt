package com.example.checadorccl

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.checadorccl.api.ApiClient
import com.example.checadorccl.utils.BiometricAuthManager
import com.example.checadorccl.utils.LocationHelper
import com.google.android.material.card.MaterialCardView
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

class PermitRequestFragment : Fragment() {

    private lateinit var txtSelectedDates: TextView
    private lateinit var txtFileName: TextView
    private lateinit var dropdownType: AutoCompleteTextView
    private lateinit var editObservations: TextInputEditText

    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null
    private var selectedFileUri: Uri? = null

    private lateinit var biometricManager: BiometricAuthManager
    private lateinit var locationHelper: LocationHelper

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                val fileName = getFileName(uri)
                txtFileName.text = fileName
                txtFileName.setTextColor(resources.getColor(R.color.color_text_dark, null))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_permit_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        biometricManager = BiometricAuthManager(requireContext(), fragment = this)
        locationHelper = LocationHelper(requireContext())

        txtSelectedDates = view.findViewById(R.id.txt_selected_dates)
        txtFileName = view.findViewById(R.id.txt_file_name)
        dropdownType = view.findViewById(R.id.dropdown_permit_type)
        editObservations = view.findViewById(R.id.edit_observations)
        val cardDatePicker = view.findViewById<MaterialCardView>(R.id.card_date_picker)
        val btnUpload = view.findViewById<Button>(R.id.btn_upload_file)
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit_permit)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel_permit)

        val permitTypes = listOf(
            "Vacaciones",
            "Permiso Con Goce de Sueldo",
            "Permiso Sin Goce de Sueldo",
            "Incapacidad",
            "Comisión",
            "Licencia"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, permitTypes)
        dropdownType.setAdapter(adapter)

        cardDatePicker.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Selecciona el rango")
                .setSelection(
                    androidx.core.util.Pair(
                        MaterialDatePicker.todayInUtcMilliseconds(),
                        MaterialDatePicker.todayInUtcMilliseconds()
                    )
                )
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedStartDate = selection.first
                selectedEndDate = selection.second

                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")

                val start = format.format(Date(selection.first))
                val end = format.format(Date(selection.second))

                txtSelectedDates.text = "$start  al  $end"
                txtSelectedDates.setTextColor(resources.getColor(R.color.color_primary_ccl, null))
                txtSelectedDates.typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }

        btnUpload.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            val mimeTypes = arrayOf("image/*", "application/pdf")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            filePickerLauncher.launch(intent)
        }

        btnSubmit.setOnClickListener {
            validateAndSubmit()
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun validateAndSubmit() {
        val tipo = dropdownType.text.toString()

        if (tipo.isEmpty()) {
            ToastHelper.show(requireContext(), "Selecciona un tipo de permiso", true)
            return
        }
        if (selectedStartDate == null) {
            ToastHelper.show(requireContext(), "Selecciona las fechas", true)
            return
        }

        biometricManager.authenticate(
            title = "Firmar Solicitud",
            subtitle = "Confirma la solicitud de $tipo"
        ) {
            ToastHelper.show(requireContext(), "Enviando solicitud...")

            locationHelper.getCurrentLocation { lat, long ->
                enviarDatosAlServidor(tipo, lat, long)
            }
        }
    }

    private fun enviarDatosAlServidor(tipo: String, lat: Double, long: Double) {
        val prefs = requireContext().getSharedPreferences("ChecadorPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("ACCESS_TOKEN", null) ?: return

        // 1. Generar lista de fechas (Array de Strings YYYY-MM-DD)
        val listaFechas = obtenerListaFechas(selectedStartDate!!, selectedEndDate!!)
        val fechasJson = Gson().toJson(listaFechas)

        // 2. Crear RequestBodies
        val tipoPart = tipo.toRequestBody("text/plain".toMediaTypeOrNull())
        val fechasPart = fechasJson.toRequestBody("text/plain".toMediaTypeOrNull())
        val obsPart = editObservations.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        // 3. Preparar Archivo
        var bodyArchivo: MultipartBody.Part? = null
        if (selectedFileUri != null) {
            val file = getFileFromUri(selectedFileUri!!)
            if (file != null) {
                val mimeType = requireContext().contentResolver.getType(selectedFileUri!!) ?: "application/pdf"
                val reqFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                bodyArchivo = MultipartBody.Part.createFormData("archivo", file.name, reqFile)
            }
        }

        // 4. Llamada a la API
        lifecycleScope.launch {
            try {
                val response = ApiClient.service.requestPermit(
                    token = "Bearer $token",
                    lat = lat.toString(),
                    long = long.toString(),
                    tipo = tipoPart,
                    fechasJson = fechasPart,
                    observaciones = obsPart,
                    archivo = bodyArchivo
                )

                if (response.isSuccessful) {
                    ToastHelper.show(requireContext(), "¡Solicitud enviada con éxito!")
                    parentFragmentManager.popBackStack() // Regresar
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e("API_ERROR", "Error ${response.code()}: $errorMsg")
                    ToastHelper.show(requireContext(), "Error al enviar: ${response.code()}", true)
                }

            } catch (e: Exception) {
                Log.e("API_ERROR", "Fallo de conexión", e)
                ToastHelper.show(requireContext(), "Error de conexión", true)
            }
        }
    }

    // Genera todas las fechas intermedias entre inicio y fin
    private fun obtenerListaFechas(startMillis: Long, endMillis: Long): List<String> {
        val fechas = mutableListOf<String>()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = startMillis

        val endCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        endCalendar.timeInMillis = endMillis

        // Formato que espera Django: YYYY-MM-DD
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        formato.timeZone = TimeZone.getTimeZone("UTC")

        while (!calendar.after(endCalendar)) {
            fechas.add(formato.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return fechas
    }

    private fun getFileFromUri(uri: Uri): File? {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(requireContext().cacheDir, "temp_permit_file")
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

    // Función auxiliar para nombre visual del archivo
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = cursor.getString(index)
                }
            } finally { cursor?.close() }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) result = result?.substring(cut + 1)
        }
        return result ?: "Archivo adjunto"
    }
}