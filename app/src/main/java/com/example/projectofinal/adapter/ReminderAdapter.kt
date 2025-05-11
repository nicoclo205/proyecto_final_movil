package com.example.projectofinal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectofinal.R
import com.example.projectofinal.model.Reminder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderAdapter(
    private var reminders: MutableList<Reminder>
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    private var onReminderClickListener: OnReminderClickListener? = null
    private var onCompletedChangeListener: OnCompletedChangeListener? = null

    interface OnReminderClickListener {
        fun onReminderClick(reminder: Reminder, position: Int)
    }

    interface OnCompletedChangeListener {
        fun onCompletedChange(reminder: Reminder, isCompleted: Boolean, position: Int)
    }

    fun setOnReminderClickListener(listener: OnReminderClickListener) {
        this.onReminderClickListener = listener
    }

    fun setOnCompletedChangeListener(listener: OnCompletedChangeListener) {
        this.onCompletedChangeListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]
        holder.bind(reminder)
    }

    override fun getItemCount(): Int = reminders.size

    fun updateReminders(newReminders: List<Reminder>) {
        this.reminders.clear()
        this.reminders.addAll(newReminders)
        notifyDataSetChanged()
    }

    fun getReminders(): List<Reminder> = reminders

    inner class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        private val checkboxCompleted: CheckBox = itemView.findViewById(R.id.checkboxCompleted)

        fun bind(reminder: Reminder) {
            tvTitle.text = reminder.title
            tvDescription.text = reminder.description
            
            // Formatear fecha y hora
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            val dateStr = dateFormat.format(Date(reminder.date))
            
            // Convertir el tiempo (en milisegundos desde el inicio del día) a un formato legible
            val timeDate = Date(reminder.time)
            val timeStr = timeFormat.format(timeDate)
            
            tvDateTime.text = "$dateStr $timeStr"
            
            // Configurar el estado del checkbox
            checkboxCompleted.isChecked = reminder.completed
            
            // Cambiar el aspecto si está completado
            if (reminder.completed) {
                tvTitle.alpha = 0.5f
                tvDescription.alpha = 0.5f
                tvDateTime.alpha = 0.5f
            } else {
                tvTitle.alpha = 1.0f
                tvDescription.alpha = 1.0f
                tvDateTime.alpha = 1.0f
            }
            
            // Configurar el listener del checkbox
            checkboxCompleted.setOnCheckedChangeListener { _, isChecked ->
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onCompletedChangeListener?.onCompletedChange(
                        reminders[adapterPosition],
                        isChecked,
                        adapterPosition
                    )
                }
            }
            
            // Configurar clic en el elemento
            itemView.setOnClickListener {
                val adapterPosition = bindingAdapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onReminderClickListener?.onReminderClick(
                        reminders[adapterPosition],
                        adapterPosition
                    )
                }
            }
        }
    }
}