package com.example.checadorccl.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // CAMBIA ESTA IP POR LA DE TU PC si usas celular físico
    // Si usas emulador, usa "http://10.0.2.2:8000/api/"
    private const val BASE_URL = "https://localhost/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Para ver en el Logcat qué enviamos y recibimos
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS) // Tiempo espera conexión
        .readTimeout(30, TimeUnit.SECONDS)    // Tiempo espera respuesta
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Esta es la instancia que usaremos en las Activities
    val service: ApiService = retrofit.create(ApiService::class.java)
}