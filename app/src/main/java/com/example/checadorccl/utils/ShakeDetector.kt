package com.example.checadorccl.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Clase auxiliar para detectar sacudidas del dispositivo usando el acelerómetro.
 * @param onShake Función lambda que se ejecutará cuando se detecte la sacudida.
 */
class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    // Umbral de aceleración para considerar que es una sacudida
    // 2.7F significa 2.7 veces la fuerza de gravedad (G).
    // Ajusta este valor si sientes que es muy sensible (súbelo) o muy duro (bájalo).
    private val SHAKE_THRESHOLD_GRAVITY = 2.7F

    // Tiempo mínimo entre sacudidas para evitar disparar múltiples veces seguidas
    private val SHAKE_SLOP_TIME_MS = 500

    private var shakeTimestamp: Long = 0

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No necesitamos hacer nada aquí
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Normalizar por gravedad terrestre
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            // Calcular fuerza G total (raíz cuadrada de la suma de cuadrados)
            val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            // Si la fuerza supera el umbral
            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val now = System.currentTimeMillis()

                // Verificar que haya pasado suficiente tiempo desde la última sacudida
                if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }

                shakeTimestamp = now

                // ¡Sacudida detectada! Ejecutar la acción
                onShake()
            }
        }
    }
}