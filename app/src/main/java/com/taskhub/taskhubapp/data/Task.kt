package com.taskhub.taskhubapp.data

/**
 * Data Transfer Object (DTO) para la entidad Task.
 * Esta estructura debe coincidir con el modelo Task.java del Backend.
 */
data class Task(
    // ID único de la tarea (puede ser nulo al crear una nueva tarea)
    val id: Long? = null,

    // Título o descripción de la tarea
    val title: String,

    // Si la tarea está completada o pendiente
    val completed: Boolean = false,

    // --- NUEVOS CAMPOS ---

    // La fecha de Java (LocalDate) se mapea como String en Kotlin para la comunicación REST
    // Formato esperado: yyyy-MM-dd
    val dueDate: String,

    // Estado de la tarea: PENDIENTE, EN PROGRESO, COMPLETADA
    val status: String,

    // Nivel de Prioridad: BAJA, MEDIA, ALTA
    val priority: String,

    // ----------------------

    // ID del usuario al que pertenece la tarea
    val userId: Long,

    // Fecha de creación (del servidor)
    val createdAt: String? = null
)