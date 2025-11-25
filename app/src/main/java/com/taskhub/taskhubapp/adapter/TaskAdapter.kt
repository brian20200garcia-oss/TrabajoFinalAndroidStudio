package com.taskhub.taskhubapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.taskhub.taskhubapp.data.Task // <-- Importación necesaria
import com.taskhub.taskhubapp.databinding.ItemTaskBinding // <-- Importación del ViewBinding

/**
 * Adaptador para el RecyclerView que muestra la lista de tareas.
 *
 * Recibe TRES parámetros de callback:
 * 1. onTaskCompletedChanged: Para el Checkbox.
 * 2. onTaskDelete: Para el Long Click.
 */
class TaskAdapter(
    private var tasks: List<Task>,
    // Callback 1: Actualización de estado (checkbox)
    private val onTaskCompletedChanged: (Task) -> Unit,
    // Callback 2: Eliminación de tarea (pulsación larga)
    private val onTaskDelete: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task, onTaskCompletedChanged: (Task) -> Unit, onTaskDelete: (Task) -> Unit) {

            binding.tvTaskTitle.text = task.title
            binding.cbTaskCompleted.isChecked = task.completed

            // Aplicar o quitar el tachado según el estado de la tarea
            if (task.completed) {
                // Aplica la bandera de tachado
                binding.tvTaskTitle.paintFlags = android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                // Quita todas las banderas de pintura
                binding.tvTaskTitle.paintFlags = 0
            }

            // 1. Manejo del Checkbox (Actualización)
            binding.cbTaskCompleted.setOnCheckedChangeListener { _, isChecked ->
                // Solo llama al callback si el nuevo estado es diferente al actual
                if (isChecked != task.completed) {
                    onTaskCompletedChanged(task)
                }
            }

            // 2. Manejo del Long Click (Eliminación)
            binding.root.setOnLongClickListener {
                // Llama al callback de eliminación
                onTaskDelete(task)
                true // Retorna true para indicar que el evento fue consumido
            }

            // Asegurarse de que el click normal no haga nada
            binding.root.setOnClickListener(null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        // Asegurarse de pasar AMBOS callbacks al método bind
        holder.bind(tasks[position], onTaskCompletedChanged, onTaskDelete)
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}