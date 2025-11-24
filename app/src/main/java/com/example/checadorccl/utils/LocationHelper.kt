package com.example.checadorccl.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Obtiene la ubicación exacta.
     * @param onLocationResult Callback que devuelve (Latitud, Longitud)
     */
    @SuppressLint("MissingPermission") // Se asume que los permisos se pidieron en Login
    fun getCurrentLocation(onLocationResult: (Double, Double) -> Unit) {

        // Usamos PRIORITY_HIGH_ACCURACY para obtener GPS real
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            object : CancellationTokenSource() {}.token
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                onLocationResult(location.latitude, location.longitude)
            } else {
                // Si falla (GPS apagado o sin señal), devolvemos 0.0
                // En un caso real, aquí mostrarías un error pidiendo prender el GPS
                onLocationResult(0.0, 0.0)
            }
        }.addOnFailureListener {
            onLocationResult(0.0, 0.0)
        }
    }
}