package com.taskhub.taskhubapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.taskhub.taskhubapp.adapter.TaskAdapter
import com.taskhub.taskhubapp.api.RetrofitClient
import com.taskhub.taskhubapp.api.TaskApiService
import com.taskhub.taskhubapp.data.Task
import com.taskhub.taskhubapp.databinding.ActivityTaskBinding
import com.taskhub.taskhubapp.viewmodel.TaskViewModel
import com.taskhub.taskhubapp.viewmodel.TaskViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskBinding
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter

    private var currentUserId: Long = -1
    private var isServiceInitialized = false

    private var selectedDueDate: Calendar = Calendar.getInstance()
    private var currentFilter: String = "Todas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Obtener datos de autenticación del Intent
        currentUserId = intent.getLongExtra("USER_ID", -1L)
        val authToken = intent.getStringExtra("AUTH_TOKEN") ?: "" // Se recibe, pero se ignora a partir de aquí
        val currentUserName = intent.getStringExtra("USER_NAME") ?: "Usuario Desconocido"

        // Mostrar el nombre del usuario en el encabezado
        binding.tvHeader.text = "TaskHub: Tareas de $currentUserName"

        // 2. Inicialización segura del ViewModel y API Service
        try {
            val apiService = RetrofitClient.instance.create(TaskApiService::class.java)
            val factory = TaskViewModelFactory(apiService)
            taskViewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]
            isServiceInitialized = true
        } catch (e: Exception) {
            Log.e("TaskActivity", "ERROR CRÍTICO: Fallo al inicializar ViewModel/API.", e)
            Toast.makeText(this, "ERROR: Fallo crítico de conexión.", Toast.LENGTH_LONG).show()
            binding.etNewTaskTitle.isEnabled = false
            binding.btnAddTask.isEnabled = false
            binding.tvStatus.text = "ERROR CRÍTICO: Servicio de red no disponible."
            isServiceInitialized = false
        }

        // 3. Configuración del flujo de la aplicación
        // La condición authToken.isNotEmpty() es redundante pero la mantenemos para asegurarnos que haya habido login
        if (isServiceInitialized && currentUserId != -1L) {
            // setAuthData ahora solo necesita el currentUserId para el ViewModel, pero le pasamos el token por si acaso
            taskViewModel.setAuthData(authToken, currentUserId)
            setupSpinnersAndPicker()
            setupRecyclerView()
            setupListeners()
            setupObservers()
        } else if (currentUserId == -1L) {
            Toast.makeText(this, "ERROR: Usuario no autenticado.", Toast.LENGTH_LONG).show()
            performLogout() // Redirigir al login si no hay ID
        } else {
            binding.progressBar.visibility = View.GONE
        }

        // Conexión del botón de Cerrar Sesión
        binding.btnLogOut.setOnClickListener {
            performLogout()
        }
    }

    private fun setupSpinnersAndPicker() {
        // --- 1. CONFIGURACIÓN DEL SPINNER DE PRIORIDAD ---
        val priorityOptions = arrayOf("ALTA", "MEDIA", "BAJA")
        val priorityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorityOptions)
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPriority.adapter = priorityAdapter
        // Seleccionar "MEDIA" por defecto
        binding.spinnerPriority.setSelection(priorityOptions.indexOf("MEDIA"))

        // --- 2. CONFIGURACIÓN DEL SPINNER DE ESTADO ---
        val statusOptions = arrayOf("PENDIENTE", "EN PROGRESO", "COMPLETADA")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter

        // --- 3. CONFIGURACIÓN DEL FILTRO DE TAREAS ---
        val filterOptions = arrayOf("Todas", "Hoy", "Próxima Semana", "Alta Prioridad", "Pendientes")
        val filterAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilter.adapter = filterAdapter

        // Listener para aplicar el filtro
        binding.spinnerFilter.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilter = filterOptions[position]
                taskViewModel.applyFilter(currentFilter)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // --- 4. CONFIGURACIÓN DEL DATE PICKER ---
        updateDateLabel() // Establecer la fecha de hoy por defecto

        binding.btnPickDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val year = selectedDueDate.get(Calendar.YEAR)
        val month = selectedDueDate.get(Calendar.MONTH)
        val day = selectedDueDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDueDate.set(selectedYear, selectedMonth, selectedDay)
                updateDateLabel()
            }, year, month, day)
        datePickerDialog.show()
    }

    private fun updateDateLabel() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.tvDateLabel.text = dateFormat.format(selectedDueDate.time)
    }

    private fun setupRecyclerView() {
        // Inicialización del adaptador con los TRES callbacks requeridos (actualización y borrado)
        taskAdapter = TaskAdapter(
            tasks = emptyList(),
            onTaskCompletedChanged = { task ->
                taskViewModel.toggleTaskCompletion(task)
            },
            onTaskDelete = { task ->
                task.id?.let { taskId ->
                    Toast.makeText(this, "Eliminando tarea: ${task.title}", Toast.LENGTH_SHORT).show()
                    taskViewModel.deleteTask(taskId)
                }
            }
        )
        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@TaskActivity)
            adapter = taskAdapter
        }
    }

    private fun setupListeners() {
        binding.btnAddTask.setOnClickListener {
            val title = binding.etNewTaskTitle.text.toString().trim()

            if (title.isNotEmpty() && isServiceInitialized) {
                // Obtener valores de la UI
                val priority = binding.spinnerPriority.selectedItem.toString()
                val status = binding.spinnerStatus.selectedItem.toString()

                // Formatear la fecha a String para Retrofit (formato yyyy-MM-dd)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dueDate = dateFormat.format(selectedDueDate.time)

                // Crear la nueva tarea (con los nuevos campos)
                taskViewModel.createTask(title, dueDate, status, priority)

                // Limpiar UI después de la creación
                binding.etNewTaskTitle.text.clear()
                // Restaurar fecha a hoy para la siguiente tarea
                selectedDueDate = Calendar.getInstance()
                updateDateLabel()
                binding.spinnerPriority.setSelection(1) // MEDIA por defecto
                binding.spinnerStatus.setSelection(0) // PENDIENTE por defecto

            } else if (!isServiceInitialized) {
                Toast.makeText(this, "La red está inactiva.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Escribe un título para la tarea.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupObservers() {
        taskViewModel.tasks.observe(this) { tasks ->
            // El ViewModel ahora emite la lista ya filtrada
            taskAdapter.updateTasks(tasks)
            binding.progressBar.visibility = View.GONE
        }

        taskViewModel.statusMessage.observe(this) { message ->
            binding.tvStatus.text = message
        }
    }

    private fun performLogout() {
        Toast.makeText(this, "Cerrando sesión...", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}