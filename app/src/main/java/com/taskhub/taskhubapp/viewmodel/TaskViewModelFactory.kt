package com.taskhub.taskhubapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.taskhub.taskhubapp.api.TaskApiService // <-- ¡CORREGIDO!

/**
 * Factory para crear instancias de TaskViewModel.
 */
class TaskViewModelFactory(private val apiService: TaskApiService) : ViewModelProvider.Factory {

    // Método principal para crear el ViewModel
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Comprueba si la clase solicitada es TaskViewModel
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Devuelve una nueva instancia de TaskViewModel, pasándole el apiService
            return TaskViewModel(apiService) as T
        }
        // Lanza una excepción si se pide una clase de ViewModel incorrecta
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}