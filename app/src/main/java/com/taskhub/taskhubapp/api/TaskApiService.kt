package com.taskhub.taskhubapp.api

import com.taskhub.taskhubapp.data.Task
import com.taskhub.taskhubapp.data.User
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz de Retrofit para interactuar con los endpoints REST del Backend TaskHub.
 * Nota: El token JWT simulado se ha ELIMINADO de las llamadas de tareas para evitar el 403,
 * ya que la seguridad del servidor está configurada como 'permitAll' para desarrollo.
 */
interface TaskApiService {

    // ------------------- ENDPOINTS DE AUTENTICACIÓN -------------------

    @POST("api/users/register")
    suspend fun registerUser(@Body user: User): Response<User>

    @POST("api/users/login")
    suspend fun loginUser(@Body credentials: Map<String, String>): Response<User>

    // ------------------- ENDPOINTS DE TAREAS (SIN ENCABEZADO DE AUTORIZACIÓN) -------------------

    @GET("api/tasks/{userId}")
    suspend fun getTasks(
        @Path("userId") userId: Long
    ): Response<List<Task>>

    @POST("api/tasks")
    suspend fun createTask(
        @Body task: Task
    ): Response<Task>

    @PUT("api/tasks/{taskId}")
    suspend fun updateTask(
        @Path("taskId") taskId: Long,
        @Body task: Task
    ): Response<Task>

    @DELETE("api/tasks/{taskId}")
    suspend fun deleteTask(
        @Path("taskId") taskId: Long
    ): Response<Unit>
}