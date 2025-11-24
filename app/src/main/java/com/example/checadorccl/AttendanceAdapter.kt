package com.example.checadorccl

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.checadorccl.models.AttendanceRecord

class AttendanceAdapter(
    private val records: List<AttendanceRecord>,
    private val onActionClick: (AttendanceRecord, String) -> Unit // Callback para manejar la navegación
) : RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDate: TextView = view.findViewById(R.id.txt_record_date)
        val txtStatus: TextView = view.findViewById(R.id.txt_status_summary)
        val txtCheckIn: TextView = view.findViewById(R.id.txt_check_in)
        val txtCheckOut: TextView = view.findViewById(R.id.txt_check_out)
        val txtTotalHours: TextView = view.findViewById(R.id.txt_total_hours)
        val layoutIncidences: LinearLayout = view.findViewById(R.id.layout_incidences)
        val btnJustify: Button = view.findViewById(R.id.btn_justify_incidence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]

        holder.txtDate.text = record.fecha
        holder.txtStatus.text = record.estadoResumen
        holder.txtCheckIn.text = record.entrada
        holder.txtCheckOut.text = record.salida
        holder.txtTotalHours.text = record.horasTotales

        // Colores de estado
        val background = holder.txtStatus.background as GradientDrawable
        when (record.estadoResumen) {
            "Aprobado", "Pagado", "Justificado" -> background.setColor(Color.parseColor("#4CAF50")) // Verde
            "En Revision" -> background.setColor(Color.parseColor("#FF9800")) // Naranja
            "Rechazado", "Falta" -> background.setColor(Color.parseColor("#F44336")) // Rojo
            else -> background.setColor(Color.parseColor("#2196F3")) // Azul default
        }

        // Incidencias dinámicas
        holder.layoutIncidences.removeAllViews()
        var tipoPrincipal = ""

        if (record.incidencias.isNotEmpty()) {
            tipoPrincipal = record.incidencias[0].nombre // Usamos el primero para la lógica
            for (incidencia in record.incidencias) {
                val textView = TextView(holder.itemView.context)
                textView.text = "${incidencia.nombre}  "
                textView.textSize = 13f
                textView.setTypeface(null, Typeface.BOLD)
                try {
                    textView.setTextColor(Color.parseColor(incidencia.colorHex))
                } catch (e: Exception) {
                    textView.setTextColor(Color.RED)
                }
                holder.layoutIncidences.addView(textView)
            }
        } else {
            val textView = TextView(holder.itemView.context)
            textView.text = "Sin incidencias"
            textView.textSize = 12f
            textView.setTextColor(Color.GRAY)
            holder.layoutIncidences.addView(textView)
        }

        // LÓGICA DE BOTONES DE ACCIÓN
        if (record.requiereJustificacion) {
            holder.btnJustify.visibility = View.VISIBLE
            holder.btnJustify.isEnabled = true

            // Analizamos el tipo para decidir la acción y el texto
            when {
                // CASO 1: FALTA -> Redirigir a Solicitud de Permiso
                tipoPrincipal.contains("Falta", true) -> {
                    holder.btnJustify.text = "Solicitar Justificante"
                    holder.btnJustify.setOnClickListener {
                        onActionClick(record, "SOLICITUD")
                    }
                }

                // CASO 2: RETRASO/SALIDA -> Ir a Formulario Completo
                tipoPrincipal.contains("Retraso", true) || tipoPrincipal.contains("Salida", true) -> {
                    holder.btnJustify.text = "Justificar Tiempo"
                    holder.btnJustify.setOnClickListener {
                        onActionClick(record, "JUSTIFICACION")
                    }
                }

                // CASO 3: OMISIÓN -> Ir a Formulario Simple
                tipoPrincipal.contains("Omisión", true) -> {
                    holder.btnJustify.text = "Justificar Omisión"
                    holder.btnJustify.setOnClickListener {
                        onActionClick(record, "JUSTIFICACION")
                    }
                }

                // CUALQUIER OTRA COSA RARA -> Ocultar
                else -> {
                    holder.btnJustify.visibility = View.GONE
                }
            }
        } else {
            // Si no requiere justificación, ocultamos el botón
            holder.btnJustify.visibility = View.GONE
        }
    }

    override fun getItemCount() = records.size
}