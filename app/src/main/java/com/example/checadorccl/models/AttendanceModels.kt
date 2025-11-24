package com.example.checadorccl.models

data class AttendanceRecord(
    val id: Int,
    val fecha: String,       // "Miércoles, 15/10/2025"
    val entrada: String,     // "08:30"
    val salida: String,      // "17:00"
    val horasTotales: String,// "8.5h"
    val estadoResumen: String, // "Aprobado", "En Revisión", "Pendiente"
    val incidencias: List<IncidenceType>, // Lista de problemas del día
    val requiereJustificacion: Boolean
)

data class IncidenceType(
    val id: Int,
    val nombre: String, // "Retraso", "Omisión Salida"
    val colorHex: String // "#FF9800" (Naranja), "#F44336" (Rojo)
)