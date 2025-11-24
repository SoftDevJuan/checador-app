package com.example.checadorccl.utils

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.checadorccl.R

object ToastHelper {
    fun show(context: Context, message: String, isError: Boolean = false) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.toast_custom, null)

        val text: TextView = layout.findViewById(R.id.toast_text)
        val image: ImageView = layout.findViewById(R.id.toast_icon)

        text.text = message

        // 1. Fondo siempre Blanco (Limpio)
        layout.background.setTint(Color.WHITE)

        if (isError) {
            // ERROR: Texto Rojo oscuro y el icono de alerta también rojo
            val errorColor = Color.parseColor("#D32F2F") // Rojo Material
            text.setTextColor(errorColor)
            image.setImageResource(android.R.drawable.ic_dialog_alert)
            image.setColorFilter(errorColor) // Teñimos el icono de alerta
        } else {
            // NORMAL: Texto Negro y el logo con sus colores originales
            text.setTextColor(Color.BLACK)
            image.setImageResource(R.drawable.ic_logo_ccl)
            image.clearColorFilter() // Quitamos cualquier tinte para ver el logo a color
        }

        val toast = Toast(context)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
}