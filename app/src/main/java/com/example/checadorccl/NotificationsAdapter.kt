package com.example.checadorccl

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.checadorccl.api.models.NotificationResponse
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationsAdapter(
    private val notifications: List<NotificationResponse>,
    private val onClick: (NotificationResponse) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtOrigin: TextView = view.findViewById(R.id.txt_notif_origin)
        val txtDate: TextView = view.findViewById(R.id.txt_notif_date)
        val txtBody: TextView = view.findViewById(R.id.txt_notif_body)
        val card: CardView = view as CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notif = notifications[position]

        holder.txtOrigin.text = notif.origen ?: "Sistema"
        holder.txtBody.text = notif.mensaje
        holder.txtDate.text = formatearFecha(notif.fecha)

        // Estilo visual: Azulito si no se ha leído, Blanco si ya se leyó
        if (notif.leido) {
            holder.card.setCardBackgroundColor(Color.WHITE)
            holder.txtBody.typeface = android.graphics.Typeface.DEFAULT
        } else {
            holder.card.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
            holder.txtBody.typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        holder.itemView.setOnClickListener {
            onClick(notif)
        }
    }

    override fun getItemCount() = notifications.size

    private fun formatearFecha(fechaStr: String): String {
        try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("dd MMM HH:mm", Locale("es", "MX"))
            val date = input.parse(fechaStr)
            return output.format(date!!)
        } catch (e: Exception) {
            return fechaStr
        }
    }
}