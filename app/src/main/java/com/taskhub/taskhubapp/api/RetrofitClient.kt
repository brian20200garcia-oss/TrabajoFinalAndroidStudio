package com.taskhub.taskhubapp.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Singleton para Retrofit.
 * Configura la URL base y el convertidor GSON con un Timeout explícito.
 */

private const val BASE_URL = "http://10.0.2.2:8080/"

object RetrofitClient {

    // Configuración del cliente HTTP para manejar Timeouts
    private val okHttpClient = OkHttpClient.Builder()
        // Establecer un timeout de 30 segundos para la conexión
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Asignar el cliente con el timeout
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}