package com.example.checadorccl

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.checadorccl.api.models.IncidenciaResponse

class IncidenceDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incidence_detail)

        // Recibir objeto serializable
        val incidencia = intent.getSerializableExtra("INCIDENCIA_OBJ") as? IncidenciaResponse

        if (incidencia == null) {
            finish()
            return
        }

        // Vincular vistas
        val txtType = findViewById<TextView>(R.id.txtDetailType)
        val txtDate = findViewById<TextView>(R.id.txtDetailDate)
        val txtStatus = findViewById<TextView>(R.id.txtDetailStatus)
        val txtTimeInfo = findViewById<TextView>(R.id.txtDetailTimeInfo)
        val txtTotal = findViewById<TextView>(R.id.txtDetailTotalHours)
        val txtJustif = findViewById<TextView>(R.id.txtDetailJustification)
        val txtObs = findViewById<TextView>(R.id.txtDetailObservations)

        // --- LLENAR DATOS ---
        txtType.text = incidencia.tipo ?: "Incidencia"
        txtDate.text = incidencia.fecha
        txtStatus.text = incidencia.estado ?: "Pendiente"

        // Colores
        val color = when(incidencia.estado) {
            "Aprobado", "Pagado" -> "#4CAF50" // Verde
            "Rechazado", "Falta" -> "#F44336" // Rojo
            else -> "#FF9800" // Naranja
        }
        txtStatus.setBackgroundColor(Color.parseColor(color))

        // Tiempos
        txtTimeInfo.text = "Entrada: ${incidencia.entrada}  |  Salida: ${incidencia.salida}"
        txtTotal.text = "Tiempo Total: ${incidencia.totalHoras}"

        // Justificación y Aprobaciones
        val sb = StringBuilder()
        if (incidencia.justificacion != null) {
            sb.append("Motivo: ${incidencia.justificacion.motivo}\n\n")

            val jefe = if(incidencia.justificacion.aprobadoJefe) "✅ Aprobado" else "⏳ Pendiente"
            val rh = if(incidencia.justificacion.aprobadoRH) "✅ Aprobado" else "⏳ Pendiente"

            sb.append("Autorización Jefe: $jefe\n")
            sb.append("Autorización RH: $rh")

            // Deuda (si existe)
            if (incidencia.deuda != null) {
                sb.append("\n\n--- DEUDA DE TIEMPO ---\n")
                sb.append("Debe: ${incidencia.deuda.tiempoDeuda}\n")
                sb.append("Pagado: ${incidencia.deuda.tiempoPagado}")
            }
        } else {
            sb.append("No se ha enviado justificación aún.")
        }

        txtJustif.text = sb.toString()
        txtObs.text = "Observaciones Generales: ${incidencia.observaciones ?: "Ninguna"}"

        findViewById<Button>(R.id.btnClose).setOnClickListener { finish() }
    }
}