package com.example.checadorccl.api.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class IncidenciaResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("tipo") val tipo: String?,
    @SerializedName("estado") val estado: String?,

    @SerializedName("entrada_fmt") val entrada: String?,
    @SerializedName("salida_fmt") val salida: String?,
    @SerializedName("total_horas") val totalHoras: String?,

    @SerializedName("requiere_justificacion") val requiereJustificacion: Boolean,
    @SerializedName("justificado") val justificado: Boolean,
    @SerializedName("observaciones") val observaciones: String?,

    // --- NUEVOS CAMPOS PARA DETALLES ---
    @SerializedName("justificacion_data") val justificacion: JustificacionData?,
    @SerializedName("deuda_data") val deuda: DeudaData?
) : Serializable

data class JustificacionData(
    @SerializedName("motivo") val motivo: String?,
    @SerializedName("aprobado_director") val aprobadoJefe: Boolean,
    @SerializedName("aprobado_rh") val aprobadoRH: Boolean,
    @SerializedName("estatus") val estatusGeneral: String?
) : Serializable

data class DeudaData(
    @SerializedName("tiempo_deuda_str") val tiempoDeuda: String?,
    @SerializedName("tiempo_pagado_str") val tiempoPagado: String?,
    @SerializedName("status_pago") val statusPago: String?
) : Serializable