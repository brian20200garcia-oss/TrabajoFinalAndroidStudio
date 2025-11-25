package com.taskhub.taskhubapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * MainActivity actúa como un lanzador (Launcher Activity).
 * Su única responsabilidad es redirigir inmediatamente a la pantalla
 * de autenticación (AuthActivity) para iniciar la sesión o el registro.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No se necesita llamar a setContentView() ya que redirigimos inmediatamente.

        // Lanzamos la actividad de autenticación
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)

        // Finalizamos MainActivity para que el usuario no pueda volver atrás
        // al presionar el botón de retroceso (Back) desde AuthActivity.
        finish()
    }
}