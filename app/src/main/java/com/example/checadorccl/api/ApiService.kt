package com.example.checadorccl.api

import com.example.checadorccl.api.models.EventoResponse // Asegúrate de importar esto para el calendario
import com.example.checadorccl.api.models.IncidenciaResponse
import com.example.checadorccl.api.models.LoginRequest
import com.example.checadorccl.api.models.LoginResponse
import com.example.checadorccl.api.models.GoogleLoginRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import com.google.gson.annotations.SerializedName

interface ApiService {

    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("attendance/")
    suspend fun getAttendanceRecords(
        @Header("Authorization") token: String,
        @Query("start_date") startDate: String?,
        @Query("end_date") endDate: String?
    ): Response<List<IncidenciaResponse>>

    @GET("calendar/")
    suspend fun getCalendarEvents(
        @Header("Authorization") token: String
    ): Response<List<EventoResponse>>

    @Multipart
    @POST("justify/{id}/")
    suspend fun justifyIncidence(
        @Header("Authorization") token: String,
        @Header("X-Latitude") lat: String,
        @Header("X-Longitude") long: String,
        @Path("id") id: Int,
        @Part("comentarios") comentarios: RequestBody,
        @Part("fechas_pago") fechasPagoJson: RequestBody?,
        @Part archivo: MultipartBody.Part?
    ): Response<ResponseBody>

    // --- NUEVO ENDPOINT: SOLICITAR PERMISO ---
    @Multipart
    @POST("permit/request/")
    suspend fun requestPermit(
        @Header("Authorization") token: String,
        @Header("X-Latitude") lat: String,
        @Header("X-Longitude") long: String,
        @Part("tipo") tipo: RequestBody,
        @Part("fechas") fechasJson: RequestBody,
        @Part("observaciones") observaciones: RequestBody,
        @Part archivo: MultipartBody.Part?
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("device/register/")
    suspend fun registerDevice(
        @Header("Authorization") token: String,
        @Field("token") fcmToken: String
    ): Response<ResponseBody>

    @GET("notifications/")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<List<com.example.checadorccl.api.models.NotificationResponse>>

    @GET("notifications/{id}/detail/")
    suspend fun getNotificationDetail(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<com.example.checadorccl.api.models.NotificationDetailResponse>


    @POST("auth/google/")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<LoginResponse>

}