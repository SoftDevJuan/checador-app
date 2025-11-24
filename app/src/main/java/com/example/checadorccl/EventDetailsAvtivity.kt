package com.example.checadorccl

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.checadorccl.api.models.EventoResponse
import java.text.SimpleDateFormat
import java.util.Locale

class EventDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_details)

        // Recibir objeto serializable
        // Nota: Si tu Android es muy nuevo (API 33+), se usa getSerializableExtra(key, class)
        // Pero este método es compatible con versiones anteriores
        val evento = intent.getSerializableExtra("EVENTO_OBJ") as? EventoResponse
        val colorHex = intent.getStringExtra("EVENT_COLOR") ?: "#F37021"

        if (evento == null) {
            finish()
            return
        }

        val txtTitle = findViewById<TextView>(R.id.detailEventTitle)
        val txtStatus = findViewById<TextView>(R.id.detailEventStatus)
        val viewColor = findViewById<View>(R.id.detailColorStrip)
        val txtGeneral = findViewById<TextView>(R.id.detailGeneralInfo)
        val txtDates = findViewById<TextView>(R.id.detailDatesList)
        val txtComments = findViewById<TextView>(R.id.detailCommentsList)
        val btnClose = findViewById<Button>(R.id.btnCloseDetail)

        txtTitle.text = evento.titulo
        txtStatus.text = evento.estado ?: "Desconocido"

        try {
            val color = Color.parseColor(colorHex)
            txtStatus.setBackgroundColor(color)
            viewColor.setBackgroundColor(color)
        } catch (e: Exception) { }

        // Info General
        val goceTexto = if (evento.conGoce) "Con Goce de Sueldo" else "Sin Goce de Sueldo"
        val autorizadoTexto = if (evento.autorizado) "Sí" else "No"
        txtGeneral.text = "• Tipo: $goceTexto\n• Autorizado: $autorizadoTexto" +
                (if (!evento.documentoUrl.isNullOrEmpty()) "\n• Documento adjunto: Sí" else "\n• Documento: No")

        // Lista de Fechas
        val sbFechas = StringBuilder()
        val inputFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale("es", "MX"))

        for (fechaInfo in evento.fechas) {
            try {
                val date = inputFmt.parse(fechaInfo.fecha)
                val fechaBonita = outputFmt.format(date!!).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                sbFechas.append("📅 $fechaBonita\n   Horario: ${fechaInfo.horaInicio} - ${fechaInfo.horaFin}\n\n")
            } catch (e: Exception) {
                sbFechas.append("📅 ${fechaInfo.fecha}\n")
            }
        }
        txtDates.text = if (sbFechas.isNotEmpty()) sbFechas.toString().trim() else "Sin fechas registradas"

        // Lista de Comentarios
        val sbComentarios = StringBuilder()
        evento.comentarios?.forEach { com ->
            sbComentarios.append("👤 ${com.usuario}:\n   ${com.texto}\n\n")
        }
        txtComments.text = if (sbComentarios.isNotEmpty()) sbComentarios.toString().trim() else "No hay comentarios"

        btnClose.setOnClickListener { finish() }
    }
}