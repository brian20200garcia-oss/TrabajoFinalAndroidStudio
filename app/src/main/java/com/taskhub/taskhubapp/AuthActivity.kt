package com.taskhub.taskhubapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.taskhub.taskhubapp.api.RetrofitClient
import com.taskhub.taskhubapp.api.TaskApiService
import com.taskhub.taskhubapp.data.User
import com.taskhub.taskhubapp.databinding.ActivityAuthBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var apiService: TaskApiService
    private var isRegisterMode = false
    private var isServiceInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialización del servicio de API
        try {
            apiService = RetrofitClient.instance.create(TaskApiService::class.java)
            isServiceInitialized = true
            binding.tvStatus.text = "Selecciona Login o Registro."
        } catch (e: Exception) {
            Log.e("AuthActivity", "Error al inicializar Retrofit/API Service", e)
            binding.tvStatus.text = "ERROR: Fallo de conexión crítica. Revisa el Backend."
            binding.btnAction.isEnabled = false
        }

        setupListeners()
        updateUIMode(isRegisterMode)
    }

    private fun setupListeners() {
        // Listener para el selector de modo (Login/Registro)
        binding.rgModeSelector.setOnCheckedChangeListener { _, checkedId ->
            isRegisterMode = checkedId == binding.rbRegister.id
            updateUIMode(isRegisterMode)
        }

        // Listener para el botón principal de acción
        binding.btnAction.setOnClickListener {
            if (isServiceInitialized) {
                if (isRegisterMode) {
                    handleRegistration()
                } else {
                    handleLogin()
                }
            } else {
                Toast.makeText(this, "El servicio de red no está disponible.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUIMode(isRegister: Boolean) {
        if (isRegister) {
            binding.tilName.visibility = View.VISIBLE
            binding.btnAction.text = "REGISTRARSE"
            binding.tvTitle.text = "TaskHub: Nuevo Usuario"
        } else {
            binding.tilName.visibility = View.GONE
            binding.btnAction.text = "INICIAR SESIÓN"
            binding.tvTitle.text = "TaskHub: Login"
        }
        binding.tvStatus.text = "" // Limpiar mensaje de estado al cambiar de modo
    }

    private fun handleRegistration() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Registrando..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // El DTO de usuario lleva el password para enviarlo al servidor
                val userToRegister = User(email = email, password = password, name = name)
                val response = apiService.registerUser(userToRegister)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val registeredUser = response.body()
                        val userId = registeredUser?.id ?: -1L
                        val userName = registeredUser?.name ?: "Usuario" // <-- CAPTURAR NOMBRE

                        Toast.makeText(this@AuthActivity, "Registro Exitoso! ID: $userId", Toast.LENGTH_LONG).show()

                        // Opcional: Cambiar automáticamente a modo Login
                        binding.rbLogin.isChecked = true
                        binding.tvStatus.text = "Registro exitoso. Procede a iniciar sesión."

                    } else if (response.code() == 400) {
                        binding.tvStatus.text = "Error: El email ya está registrado."
                    } else {
                        binding.tvStatus.text = "Error de registro: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvStatus.text = "Excepción de conexión: ${e.message}"
                }
            }
        }
    }

    private fun handleLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Ingresa email y contraseña.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Iniciando sesión..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Creamos un mapa para las credenciales, como espera el Backend
                val credentials = mapOf("email" to email, "password" to password)
                val response = apiService.loginUser(credentials)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val user = response.body()
                        val userId = user?.id ?: -1L
                        val userName = user?.name ?: "Usuario" // <-- CAPTURAR NOMBRE

                        if (userId != -1L) {
                            // Simulamos el token. En un entorno real, el servidor lo devolvería.
                            val authToken = "simulated_jwt_token_${userId}"
                            navigateToTaskHub(userId, authToken, userName) // <-- ENVIAR NOMBRE
                        } else {
                            binding.tvStatus.text = "Error: Respuesta de usuario inválida."
                        }
                    } else if (response.code() == 401) {
                        binding.tvStatus.text = "Error: Credenciales inválidas."
                    } else {
                        binding.tvStatus.text = "Error de login: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvStatus.text = "Excepción de conexión: ${e.message}"
                }
            }
        }
    }

    // Función de navegación actualizada para incluir el nombre
    private fun navigateToTaskHub(userId: Long, authToken: String, userName: String) {
        val intent = Intent(this, TaskActivity::class.java).apply {
            // Pasamos el ID, el token y el NOMBRE
            putExtra("USER_ID", userId)
            putExtra("AUTH_TOKEN", authToken)
            putExtra("USER_NAME", userName) // <-- Nuevo extra
        }
        startActivity(intent)
        finish()
    }
}