package com.example.checadorccl.api.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable // Importante

// Agregamos : Serializable a todas las clases
data class EventoResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("nombre_evento") val titulo: String,
    @SerializedName("tipo_visual") val tipoVisual: String,
    @SerializedName("estado") val estado: String?,
    @SerializedName("autorizado") val autorizado: Boolean,
    @SerializedName("goce") val conGoce: Boolean,
    @SerializedName("documento") val documentoUrl: String?,
    @SerializedName("fechas_info") val fechas: List<FechaInfo>,
    @SerializedName("comentarios") val comentarios: List<ComentarioInfo>?
) : Serializable

data class FechaInfo(
    @SerializedName("fecha") val fecha: String,
    @SerializedName("inicia_fmt") val horaInicio: String,
    @SerializedName("termina_fmt") val horaFin: String
) : Serializable

data class ComentarioInfo(
    @SerializedName("id") val id: Int,
    @SerializedName("usuario_nombre") val usuario: String,
    @SerializedName("comentario") val texto: String,
    @SerializedName("fecha_comentario") val fecha: String
) : Serializable