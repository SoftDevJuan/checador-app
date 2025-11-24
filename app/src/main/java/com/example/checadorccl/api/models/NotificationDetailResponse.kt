package com.example.checadorccl.api.models

import com.google.gson.annotations.SerializedName

data class NotificationDetailResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("tipo_objeto") val tipoObjeto: String, // "INCIDENCIA" o "EVENTO"

    // Gson intentará parsear 'data' a uno de estos dos dependiendo de cómo lo usemos.
    // Como viene como objeto genérico "data", lo más fácil es usar JsonElement
    // y parsearlo manualmente, o tener campos separados si el backend los enviara separados.
    // TRUCO: Usaremos `com.google.gson.JsonElement` para parsearlo dinámicamente en el Fragmento.
    @SerializedName("data") val data: com.google.gson.JsonElement?
)