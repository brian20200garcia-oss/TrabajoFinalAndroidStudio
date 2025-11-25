package com.taskhub.taskhubapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskhub.taskhubapp.api.TaskApiService
import com.taskhub.taskhubapp.data.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * ViewModel para gestionar la lógica de las tareas: cargar, crear, actualizar y eliminar, más filtros.
 */
class TaskViewModel(private val apiService: TaskApiService) : ViewModel() {

    // Lista de todas las tareas SIN FILTRAR (el cache)
    private var allTasks = emptyList<Task>()
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    // El token ya no se usa, solo necesitamos el ID del usuario
    private var currentUserId: Long = -1
    private var currentFilter: String = "Todas"

    /**
     * Establece los datos de autenticación después de un login exitoso (solo se necesita el ID).
     */
    fun setAuthData(token: String, userId: Long) {
        // Ignoramos el token, pero usamos el ID
        this.currentUserId = userId
        loadTasks()
    }

    /**
     * Aplica un filtro a la lista de tareas en caché y actualiza _tasks.
     * @param filterName Nombre del filtro seleccionado ("Hoy", "Alta Prioridad", etc.)
     */
    fun applyFilter(filterName: String) {
        currentFilter = filterName
        val filteredList = when (filterName) {
            "Hoy" -> filterByDateRange(0, 0)
            "Próxima Semana" -> filterByDateRange(1, 7)
            "Alta Prioridad" -> allTasks.filter { it.priority.uppercase() == "ALTA" && !it.completed } // Solo pendientes de alta prioridad
            "Pendientes" -> allTasks.filter { !it.completed }
            else -> allTasks // Todas
        }
        _tasks.value = filteredList
    }

    private fun filterByDateRange(minDays: Long, maxDays: Long): List<Task> {
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

        return allTasks.filter { task ->
            try {
                val taskDate = LocalDate.parse(task.dueDate, dateFormatter)
                val daysUntil = ChronoUnit.DAYS.between(today, taskDate)

                // Debe estar dentro del rango (minDays a maxDays)
                daysUntil >= minDays && daysUntil <= maxDays
            } catch (e: Exception) {
                false // Ignorar tareas con formato de fecha incorrecto
            }
        }.sortedBy { LocalDate.parse(it.dueDate, dateFormatter) } // Ordenar por fecha
    }

    /**
     * Carga las tareas, las guarda en la caché (allTasks) y luego aplica el filtro actual.
     * Ya no usa el token.
     */
    fun loadTasks() {
        if (currentUserId == -1L) return

        _statusMessage.value = "Cargando tareas..."
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // LLamada sin el parámetro token
                    val response = apiService.getTasks(currentUserId)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            allTasks = response.body() ?: emptyList()
                            applyFilter(currentFilter)
                            _statusMessage.value = "Tareas cargadas correctamente."
                        } else {
                            _statusMessage.value = "Error al cargar tareas: ${response.code()}"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Error de conexión: ${e.message}"
                    }
                }
            }
        }
    }

    /**
     * Crea una nueva tarea con todos los nuevos campos.
     * Ya no usa el token.
     */
    fun createTask(title: String, dueDate: String, status: String, priority: String) {
        if (currentUserId == -1L) return

        val newTask = Task(
            title = title,
            userId = currentUserId,
            dueDate = dueDate,
            status = status,
            priority = priority
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.createTask(newTask)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            _statusMessage.value = "Tarea creada con éxito."
                            loadTasks()
                        } else {
                            _statusMessage.value = "Error al crear tarea: ${response.code()}"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Error de conexión al crear tarea: ${e.message}"
                    }
                }
            }
        }
    }

    /**
     * Alterna el estado 'completed' y actualiza el estado 'status'.
     * Ya no usa el token.
     */
    fun toggleTaskCompletion(task: Task) {
        if (task.id == null || currentUserId == -1L) return

        val newCompletedState = !task.completed
        val newStatus = if (newCompletedState) "COMPLETADA" else "PENDIENTE"

        val updatedTask = task.copy(completed = newCompletedState, status = newStatus)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.updateTask(task.id, updatedTask)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            loadTasks()
                        } else {
                            _statusMessage.value = "Error al actualizar tarea: ${response.code()}"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Error de conexión al actualizar tarea: ${e.message}"
                    }
                }
            }
        }
    }

    /**
     * Elimina una tarea por su ID.
     * Ya no usa el token.
     */
    fun deleteTask(taskId: Long) {
        if (currentUserId == -1L) return

        _statusMessage.value = "Eliminando tarea $taskId..."

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.deleteTask(taskId)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            _statusMessage.value = "Tarea eliminada con éxito."
                            loadTasks()
                        } else {
                            _statusMessage.value = "Error al eliminar tarea: ${response.code()}"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _statusMessage.value = "Error de conexión al eliminar tarea: ${e.message}"
                    }
                }
            }
        }
    }
}