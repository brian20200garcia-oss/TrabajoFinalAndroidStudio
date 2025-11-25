package com.taskhub.taskhubapp.data

// DTO para manejar el registro y login (sin incluir la contraseña en la respuesta)
data class User(
    val id: Long? = null,
    val email: String,
    val password: String? = null, // Usado solo para enviar al servidor (registro/login)
    val name: String? = null,
    val createdAt: String? = null
)