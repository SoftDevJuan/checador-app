package com.example.checadorccl.utils



import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(
    private val context: Context,
    private val activity: FragmentActivity? = null, // Usar si llamamos desde Activity
    private val fragment: Fragment? = null          // Usar si llamamos desde Fragment
) {

    private val executor = ContextCompat.getMainExecutor(context)

    fun authenticate(
        title: String = "Confirmar identidad",
        subtitle: String = "Usa tu huella para continuar",
        onSuccess: () -> Unit
    ) {
        // 1. Verificar si el hardware está disponible
        val biometricManager = BiometricManager.from(context)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Todo bien, procedemos
                showPrompt(title, subtitle, onSuccess)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                ToastHelper.show(context, "Este dispositivo no tiene sensor de huella.", true)
                // En caso de no haber sensor, podrías decidir ejecutar onSuccess() o pedir PIN
                onSuccess()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                ToastHelper.show(context, "Sensor biométrico no disponible temporalmente.", true)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                ToastHelper.show(context, "No tienes huellas registradas en el celular.", true)
                // Si no hay huella, dejamos pasar (o pediríamos contraseña)
                onSuccess()
            }
        }
    }

    private fun showPrompt(title: String, subtitle: String, onSuccess: () -> Unit) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancelar")
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // ¡Autenticación Exitosa! Ejecutamos la acción
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                ToastHelper.show(context, "Error: $errString", true)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                ToastHelper.show(context, "Huella no reconocida. Intenta de nuevo.", true)
            }
        }

        // Crear el prompt dependiendo de si estamos en Fragment o Activity
        val biometricPrompt = if (fragment != null) {
            BiometricPrompt(fragment, executor, callback)
        } else {
            BiometricPrompt(activity!!, executor, callback)
        }

        biometricPrompt.authenticate(promptInfo)
    }
}