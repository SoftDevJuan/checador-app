package com.example.checadorccl.api.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("refresh") val refreshToken: String,
    @SerializedName("access") val accessToken: String,
    @SerializedName("empleado") val empleado: EmployeeData?
)

data class EmployeeData(
    @SerializedName("nombre_completo") val nombreCompleto: String,
    @SerializedName("puesto") val puesto: String,
    @SerializedName("departamento") val departamento: String,
    @SerializedName("jefe_nombre") val jefeNombre: String,
    @SerializedName("id_cda") val idCda: String?,

    // Este campo recibirá la URL completa generada por Django
    @SerializedName("foto") val fotoUrl: String?

)

