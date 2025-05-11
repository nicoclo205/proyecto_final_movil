package com.example.projectofinal

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectofinal.adapter.ReminderAdapter
import com.example.projectofinal.model.Reminder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReminderActivity : AppCompatActivity() {
    
    private lateinit var calendarView: CalendarView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvEmptyReminders: TextView
    private lateinit var btnAddReminder: Button
    private lateinit var homeButton: ImageButton
    private lateinit var orderButton: ImageButton
    private lateinit var calculatorButton: ImageButton
    private lateinit var infoButton: ImageButton
    
    private lateinit var reminderAdapter: ReminderAdapter
    private val remindersList = mutableListOf<Reminder>()
    
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingBackground: View
    
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference
    
    private var selectedDate: Long = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder)
        
        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        
        // Inicializar vistas
        calendarView = findViewById(R.id.calendarView)
        recyclerView = findViewById(R.id.recyclerReminders)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvEmptyReminders = findViewById(R.id.tvEmptyReminders)
        btnAddReminder = findViewById(R.id.btnAddReminder)
        homeButton = findViewById(R.id.homeButton)
        orderButton = findViewById(R.id.orderButton)
        calculatorButton = findViewById(R.id.calculatorButton)
        infoButton = findViewById(R.id.infoButton)
        
        progressBar = findViewById(R.id.progressBar)
        loadingBackground = findViewById(R.id.loadingBackground)
        
        // Configurar RecyclerView
        setupRecyclerView()
        
        // Configurar fecha actual como inicio
        val calendar = Calendar.getInstance()
        selectedDate = calendar.timeInMillis
        updateSelectedDateText(selectedDate)
        
        // Cargar recordatorios para la fecha actual
        loadReminders(selectedDate)
        
        // Configurar escuchas de eventos
        setupListeners()
    }
    
    private fun setupRecyclerView() {
        reminderAdapter = ReminderAdapter(remindersList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = reminderAdapter
        
        // Configurar escucha para clic en recordatorio
        reminderAdapter.setOnReminderClickListener(object : ReminderAdapter.OnReminderClickListener {
            override fun onReminderClick(reminder: Reminder, position: Int) {
                showEditReminderDialog(reminder)
            }
        })
        
        // Configurar escucha para cambio en estado de completado
        reminderAdapter.setOnCompletedChangeListener(object : ReminderAdapter.OnCompletedChangeListener {
            override fun onCompletedChange(reminder: Reminder, isCompleted: Boolean, position: Int) {
                updateReminderCompletedStatus(reminder, isCompleted)
            }
        })
    }
    
    private fun setupListeners() {
        // Escucha para cambio de fecha en el calendario
        calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.timeInMillis
            
            updateSelectedDateText(selectedDate)
            loadReminders(selectedDate)
        }
        
        // Botón para agregar recordatorio
        btnAddReminder.setOnClickListener {
            showAddReminderDialog()
        }
        
        // Botones de navegación
        homeButton.setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
            finish()
        }
        
        orderButton.setOnClickListener {
            startActivity(Intent(this, OrderActivity::class.java))
            finish()
        }
        
        calculatorButton.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
            finish()
        }
        
        infoButton.setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java))
            finish()
        }
    }
    
    private fun updateSelectedDateText(date: Long) {
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val dateStr = dateFormat.format(Date(date))
        tvSelectedDate.text = "Recordatorios para $dateStr"
    }
    
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            loadingBackground.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
            loadingBackground.visibility = View.GONE
        }
    }
    
    private fun loadReminders(date: Long) {
        showLoading(true)
        
        // Conseguir el ID del usuario actual
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showLoading(false)
            return
        }
        
        // Conseguir el inicio y el fin del día para la fecha seleccionada
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startOfDay = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        
        val endOfDay = calendar.timeInMillis
        
        // Buscar recordatorios para la fecha seleccionada
        database.child("reminders")
            .child(userId)
            .orderByChild("date")
            .startAt(startOfDay.toDouble())
            .endAt(endOfDay.toDouble())
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    remindersList.clear()
                    
                    for (reminderSnapshot in snapshot.children) {
                        val reminder = reminderSnapshot.getValue(Reminder::class.java)
                        reminder?.let {
                            remindersList.add(it)
                        }
                    }
                    
                    // Ordenar recordatorios por hora
                    remindersList.sortBy { it.time }
                    
                    // Actualizar adaptador
                    reminderAdapter.updateReminders(remindersList)
                    
                    // Mostrar mensaje si no hay recordatorios
                    if (remindersList.isEmpty()) {
                        tvEmptyReminders.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        tvEmptyReminders.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                    
                    showLoading(false)
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ReminderActivity, 
                        "Error al cargar recordatorios: ${error.message}", 
                        Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            })
    }
    
    private fun showAddReminderDialog() {
        val builder = AlertDialog.Builder(this, R.style.RoundedCornerDialog)
        
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_add_reminder, null)
        builder.setView(dialogLayout)
        
        val dialog = builder.create()
        
        // Inicializar vistas del diálogo
        val dialogTitle = dialogLayout.findViewById<TextView>(R.id.dialogTitle)
        val etTitle = dialogLayout.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogLayout.findViewById<EditText>(R.id.etDescription)
        val tvDate = dialogLayout.findViewById<TextView>(R.id.tvDate)
        val btnSelectDate = dialogLayout.findViewById<Button>(R.id.btnSelectDate)
        val tvTime = dialogLayout.findViewById<TextView>(R.id.tvTime)
        val btnSelectTime = dialogLayout.findViewById<Button>(R.id.btnSelectTime)
        val btnSaveReminder = dialogLayout.findViewById<Button>(R.id.btnSaveReminder)
        val btnDeleteReminder = dialogLayout.findViewById<Button>(R.id.btnDeleteReminder)
        val btnCancel = dialogLayout.findViewById<Button>(R.id.cancelButton)
        
        val progressBar = dialogLayout.findViewById<ProgressBar>(R.id.progressBar)
        val loadingBackground = dialogLayout.findViewById<View>(R.id.loadingBackground)
        
        // Configurar título del diálogo
        dialogTitle.text = "Agregar Recordatorio"
        
        // Ocultar botón de eliminar para agregar nuevo
        btnDeleteReminder.visibility = View.GONE
        
        // Variables para almacenar la fecha y hora seleccionadas
        var reminderDate = selectedDate
        var reminderTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis % (24 * 60 * 60 * 1000) // Solo almacenar milisegundos desde el inicio del día
        
        // Actualizar el texto de la fecha y hora inicialmente
        updateDateText(tvDate, reminderDate)
        updateTimeText(tvTime, reminderTime)
        
        // Configurar el selector de fecha
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = reminderDate
            
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                    calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
                    reminderDate = calendar.timeInMillis
                    updateDateText(tvDate, reminderDate)
                },
                year, month, day
            )
            
            datePickerDialog.show()
        }
        
        // Configurar el selector de hora
        btnSelectTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = reminderTime
            
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            
            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                    calendar.set(Calendar.MINUTE, selectedMinute)
                    reminderTime = calendar.timeInMillis % (24 * 60 * 60 * 1000)
                    updateTimeText(tvTime, reminderTime)
                },
                hour, minute, true
            )
            
            timePickerDialog.show()
        }
        
        // Configurar botón cancelar
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // Configurar botón guardar
        btnSaveReminder.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            
            // Validar título
            if (title.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese un título", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Mostrar carga
            progressBar.visibility = View.VISIBLE
            loadingBackground.visibility = View.VISIBLE
            
            // Guardar recordatorio
            saveReminder(title, description, reminderDate, reminderTime, dialog, progressBar, loadingBackground)
        }
        
        dialog.show()
        // Configurar ancho del diálogo
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    
    private fun showEditReminderDialog(reminder: Reminder) {
        val builder = AlertDialog.Builder(this, R.style.RoundedCornerDialog)
        
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_add_reminder, null)
        builder.setView(dialogLayout)
        
        val dialog = builder.create()
        
        // Inicializar vistas del diálogo
        val dialogTitle = dialogLayout.findViewById<TextView>(R.id.dialogTitle)
        val etTitle = dialogLayout.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogLayout.findViewById<EditText>(R.id.etDescription)
        val tvDate = dialogLayout.findViewById<TextView>(R.id.tvDate)
        val btnSelectDate = dialogLayout.findViewById<Button>(R.id.btnSelectDate)
        val tvTime = dialogLayout.findViewById<TextView>(R.id.tvTime)
        val btnSelectTime = dialogLayout.findViewById<Button>(R.id.btnSelectTime)
        val btnSaveReminder = dialogLayout.findViewById<Button>(R.id.btnSaveReminder)
        val btnDeleteReminder = dialogLayout.findViewById<Button>(R.id.btnDeleteReminder)
        val btnCancel = dialogLayout.findViewById<Button>(R.id.cancelButton)
        
        val progressBar = dialogLayout.findViewById<ProgressBar>(R.id.progressBar)
        val loadingBackground = dialogLayout.findViewById<View>(R.id.loadingBackground)
        
        // Configurar título del diálogo
        dialogTitle.text = "Editar Recordatorio"
        
        // Mostrar el botón de eliminar para edición
        btnDeleteReminder.visibility = View.VISIBLE
        
        // Llenar el formulario con los datos actuales
        etTitle.setText(reminder.title)
        etDescription.setText(reminder.description)
        
        // Variables para almacenar la fecha y hora seleccionadas
        var reminderDate = reminder.date
        var reminderTime = reminder.time
        
        // Actualizar el texto de la fecha y hora inicialmente
        updateDateText(tvDate, reminderDate)
        updateTimeText(tvTime, reminderTime)
        
        // Configurar el selector de fecha
        btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = reminderDate
            
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                    calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
                    reminderDate = calendar.timeInMillis
                    updateDateText(tvDate, reminderDate)
                },
                year, month, day
            )
            
            datePickerDialog.show()
        }
        
        // Configurar el selector de hora
        btnSelectTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = reminderTime
            
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            
            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                    calendar.set(Calendar.MINUTE, selectedMinute)
                    reminderTime = calendar.timeInMillis % (24 * 60 * 60 * 1000)
                    updateTimeText(tvTime, reminderTime)
                },
                hour, minute, true
            )
            
            timePickerDialog.show()
        }
        
        // Configurar botón eliminar
        btnDeleteReminder.setOnClickListener {
            // Mostrar diálogo de confirmación
            AlertDialog.Builder(this)
                .setTitle("Eliminar Recordatorio")
                .setMessage("¿Está seguro de que desea eliminar este recordatorio? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar") { _, _ ->
                    deleteReminder(reminder.id)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        
        // Configurar botón cancelar
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // Configurar botón guardar
        btnSaveReminder.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            
            // Validar título
            if (title.isEmpty()) {
                Toast.makeText(this, "Por favor ingrese un título", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Mostrar carga
            progressBar.visibility = View.VISIBLE
            loadingBackground.visibility = View.VISIBLE
            
            // Actualizar recordatorio
            updateReminder(reminder.id, title, description, reminderDate, reminderTime, reminder.completed, dialog, progressBar, loadingBackground)
        }
        
        dialog.show()
        // Configurar ancho del diálogo
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    
    private fun updateDateText(textView: TextView, date: Long) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date(date))
        textView.text = "Fecha: $dateStr"
    }
    
    private fun updateTimeText(textView: TextView, time: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        textView.text = "Hora: ${String.format("%02d:%02d", hour, minute)}"
    }
    
    private fun saveReminder(title: String, description: String, date: Long, time: Long, dialog: AlertDialog, progressBar: ProgressBar, loadingBackground: View) {
        val userId = auth.currentUser?.uid ?: return
        
        // Crear un nuevo ID para el recordatorio
        val reminderId = database.child("reminders").child(userId).push().key ?: return
        
        // Crear el objeto recordatorio
        val reminder = Reminder(
            id = reminderId,
            title = title,
            description = description,
            date = date,
            time = time,
            completed = false
        )
        
        // Guardar en la base de datos
        database.child("reminders")
            .child(userId)
            .child(reminderId)
            .setValue(reminder)
            .addOnSuccessListener {
                Toast.makeText(this, "Recordatorio guardado exitosamente", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                loadingBackground.visibility = View.GONE
                dialog.dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar recordatorio: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                loadingBackground.visibility = View.GONE
            }
    }
    
    private fun updateReminder(id: String, title: String, description: String, date: Long, time: Long, completed: Boolean, dialog: AlertDialog, progressBar: ProgressBar, loadingBackground: View) {
        val userId = auth.currentUser?.uid ?: return
        
        // Crear mapa con actualizaciones
        val updates = mapOf(
            "title" to title,
            "description" to description,
            "date" to date,
            "time" to time,
            "completed" to completed
        )
        
        // Actualizar en la base de datos
        database.child("reminders")
            .child(userId)
            .child(id)
            .updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Recordatorio actualizado exitosamente", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                loadingBackground.visibility = View.GONE
                dialog.dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar recordatorio: ${e.message}", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                loadingBackground.visibility = View.GONE
            }
    }
    
    private fun updateReminderCompletedStatus(reminder: Reminder, isCompleted: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        
        // Actualizar solo el estado de completado
        database.child("reminders")
            .child(userId)
            .child(reminder.id)
            .child("completed")
            .setValue(isCompleted)
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar estado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun deleteReminder(reminderId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        showLoading(true)
        
        // Eliminar de la base de datos
        database.child("reminders")
            .child(userId)
            .child(reminderId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Recordatorio eliminado exitosamente", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar recordatorio: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
}