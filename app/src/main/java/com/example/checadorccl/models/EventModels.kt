package com.example.checadorccl.models

import java.time.LocalDate

data class CalendarEvent(
    val id: String,
    val title: String,       // "Vacaciones", "Permiso"
    val date: LocalDate,     // Fecha del evento
    val type: EventType,     // Para el color
    val description: String  // "Aprobado por RH"
)

enum class EventType(val colorHex: String) {
    VACATION("#4CAF50"),   // Verde
    PERMISSION("#2196F3"), // Azul
    HOLIDAY("#F44336"),    // Rojo
    OTHER("#9E9E9E")       // Gris
}