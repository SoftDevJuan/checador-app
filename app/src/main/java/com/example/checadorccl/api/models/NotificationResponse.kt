package com.example.checadorccl.api.models
import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    val id: Int,
    val mensaje: String,
    val leido: Boolean,
    @SerializedName("fecha_creacion") val fecha: String,
    val origen: String
)