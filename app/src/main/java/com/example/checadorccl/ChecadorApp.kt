package com.example.checadorccl

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class ChecadorApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // ESTA LÍNEA ES LA MAGIA:
        // Fuerza a la aplicación a usar siempre el modo CLARO (Light),
        // ignorando la configuración del sistema del usuario.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}