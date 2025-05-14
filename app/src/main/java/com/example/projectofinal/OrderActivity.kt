package com.example.projectofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectofinal.adapter.OrderAdapter
import com.example.projectofinal.model.Order
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

class OrderActivity : AppCompatActivity() {

    private lateinit var buttonAdd: Button
    private lateinit var buttonSearch: Button
    private lateinit var recyclerOrders: RecyclerView
    private lateinit var textEmptyOrders: TextView
    private lateinit var homeButton: ImageButton
    private lateinit var orderButton: ImageButton
    private lateinit var calculatorButton: ImageButton
    private lateinit var recordsButton: ImageButton
    private lateinit var infoButton: ImageButton
    
    private lateinit var orderAdapter: OrderAdapter
    private val ordersList = mutableListOf<Order>()
    private val filteredOrdersList = mutableListOf<Order>() // Lista para pedidos filtrados
    
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingBackground: View
    
    // Variable para rastrear si estamos en modo búsqueda
    private var isSearchMode = false
    
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        // Initialize views
        buttonAdd = findViewById(R.id.buttonAdd)
        buttonSearch = findViewById(R.id.buttonSearch)
        recyclerOrders = findViewById(R.id.recyclerOrders)
        textEmptyOrders = findViewById(R.id.textEmptyOrders)
        homeButton = findViewById(R.id.homeButton)
        orderButton = findViewById(R.id.orderButton)
        calculatorButton = findViewById(R.id.calculatorButton)
        recordsButton = findViewById(R.id.recordsButton)
        infoButton = findViewById(R.id.infoButton)
        
        progressBar = findViewById(R.id.progressBar)
        loadingBackground = findViewById(R.id.loadingBackground)
        
        // Asegurar que la carga inicie de forma oculta
        progressBar.visibility = View.GONE
        loadingBackground.visibility = View.GONE
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Load products for later use in order creation
        loadProducts()
        
        // Load orders from Firebase
        loadOrders()
        
        // Setup button clicks
        setupButtonListeners()
    }
    
    //función para mostrar u ocultar el indicador de carga
    private fun showLoading(isLoading: Boolean){
        if(isLoading){
            progressBar.visibility = View.VISIBLE
            loadingBackground.visibility = View.VISIBLE
            
            buttonAdd.isEnabled = false
            buttonSearch.isEnabled = false
            homeButton.isEnabled = false
            orderButton.isEnabled = false
            calculatorButton.isEnabled = false
            recordsButton.isEnabled = false
            infoButton.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            loadingBackground.visibility = View.GONE
            
            buttonAdd.isEnabled = true
            buttonSearch.isEnabled = true
            homeButton.isEnabled = true
            orderButton.isEnabled = true
            calculatorButton.isEnabled = true
            recordsButton.isEnabled = true
            infoButton.isEnabled = true
        }
    }
    
    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(ordersList)
        recyclerOrders.layoutManager = LinearLayoutManager(this)
        recyclerOrders.adapter = orderAdapter
        
        // Setup click listener for order details
        orderAdapter.setOnOrderClickListener(object : OrderAdapter.OnOrderClickListener {
            override fun onOrderClick(order: Order, position: Int) {
                showOrderDetailsDialog(order)
            }
        })
    }
    
    private fun loadProducts() {
        // Ya no necesitamos cargar productos para la vista simplificada
        showLoading(false)
    }
    
    private fun loadOrders() {
        showLoading(true)
        database.child("orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ordersList.clear()
                
                for (orderSnapshot in snapshot.children) {
                    val order = orderSnapshot.getValue(Order::class.java)
                    order?.let { ordersList.add(it) }
                }
                
                // Ordenar por fecha descendente (más recientes primero)
                ordersList.sortByDescending { it.orderDate }
                
                // Si estamos en modo búsqueda, mantener los pedidos filtrados
                if (isSearchMode) {
                    // No actualizamos el adaptador ni cambiamos la visibilidad
                } else {
                    // En modo normal, mostrar todos los pedidos
                    orderAdapter.updateOrders(ordersList)
                    
                    // Show empty state if needed
                    if (ordersList.isEmpty()) {
                        textEmptyOrders.visibility = View.VISIBLE
                        recyclerOrders.visibility = View.GONE
                    } else {
                        textEmptyOrders.visibility = View.GONE
                        recyclerOrders.visibility = View.VISIBLE
                    }
                }
                
                showLoading(false)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@OrderActivity, "Error al cargar pedidos: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        })
    }
    
    private fun setupButtonListeners() {

        buttonAdd.setOnClickListener {
            showAddOrderDialog()
        }

        buttonSearch.setOnClickListener {
            showSearchOrderDialog()
        }

        homeButton.setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
            finish()
        }
        
        calculatorButton.setOnClickListener {
            startActivity(Intent(this, CalculatorActivity::class.java))
            finish()
        }
        
        recordsButton.setOnClickListener {
            startActivity(Intent(this, ReminderActivity::class.java))
            finish()
        }
        
        infoButton.setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java))
            finish()
        }
    }
    
    private fun showAddOrderDialog() {
        val builder = AlertDialog.Builder(this, R.style.RoundedCornerDialog)
        
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_add_order, null)
        builder.setView(dialogLayout)
        
        val dialog = builder.create()
        
        val etCustomerName = dialogLayout.findViewById<EditText>(R.id.etCustomerName)
        val etPhone = dialogLayout.findViewById<EditText>(R.id.etPhone)
        val etAddress = dialogLayout.findViewById<EditText>(R.id.etAddress)
        val etDescription = dialogLayout.findViewById<EditText>(R.id.etDescription)
        val etTotalAmount = dialogLayout.findViewById<EditText>(R.id.etTotalAmount)
        val btnClose = dialogLayout.findViewById<ImageButton>(R.id.btnClose)
        val btnSaveOrder = dialogLayout.findViewById<Button>(R.id.btnSaveOrder)
        
        // Close button
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        // Save order
        btnSaveOrder.setOnClickListener {
            showLoading(true)
            
            val customerName = etCustomerName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val totalAmountStr = etTotalAmount.text.toString().trim()
            
            // Validate inputs
            if (customerName.isEmpty()) {
                Toast.makeText(this, "Ingrese el nombre del cliente", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            if (phone.isEmpty()) {
                Toast.makeText(this, "Ingrese el teléfono", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            if (address.isEmpty()) {
                Toast.makeText(this, "Ingrese la dirección de entrega", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            if (description.isEmpty()) {
                Toast.makeText(this, "Ingrese una descripción del pedido", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            if (totalAmountStr.isEmpty()) {
                Toast.makeText(this, "Ingrese el monto total", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            val totalAmount = totalAmountStr.toDoubleOrNull()
            if (totalAmount == null || totalAmount <= 0) {
                Toast.makeText(this, "El monto total debe ser un número positivo", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            // Create and save the order
            val orderId = database.child("orders").push().key ?: return@setOnClickListener
            
            val order = Order(
                id = orderId,
                customerName = customerName,
                phone = phone,
                address = address,
                description = description,
                totalAmount = totalAmount,
                orderDate = Date().time,
                status = "Pendiente"
            )
            
            database.child("orders").child(orderId).setValue(order)
                .addOnSuccessListener {
                    Toast.makeText(this, "Pedido guardado exitosamente", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
        }
        
        dialog.show()
        // Set dialog width
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    

    
    private fun showOrderDetailsDialog(order: Order) {
        val builder = AlertDialog.Builder(this, R.style.RoundedCornerDialog)
        
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_order_details, null)
        builder.setView(dialogLayout)
        
        val dialog = builder.create()
        
        // Initialize views
        val tvCustomerName = dialogLayout.findViewById<TextView>(R.id.tvCustomerName)
        val tvPhone = dialogLayout.findViewById<TextView>(R.id.tvPhone)
        val tvAddress = dialogLayout.findViewById<TextView>(R.id.tvAddress)
        val tvOrderDate = dialogLayout.findViewById<TextView>(R.id.tvOrderDate)
        val tvStatus = dialogLayout.findViewById<TextView>(R.id.tvStatus)
        val tvDescription = dialogLayout.findViewById<TextView>(R.id.tvDescription)
        val tvTotalAmount = dialogLayout.findViewById<TextView>(R.id.tvTotalAmount)
        val btnClose = dialogLayout.findViewById<ImageButton>(R.id.btnClose)
        val btnEdit = dialogLayout.findViewById<Button>(R.id.btnEdit)
        val btnDelete = dialogLayout.findViewById<Button>(R.id.btnDelete)
        
        // Set order details
        tvCustomerName.text = "Cliente: ${order.customerName}"
        tvPhone.text = "Teléfono: ${order.phone}"
        tvAddress.text = "Dirección: ${order.address}"
        
        // Format and set date
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val orderDate = Date(order.orderDate)
        tvOrderDate.text = "Fecha: ${dateFormat.format(orderDate)}"
        
        tvStatus.text = "Estado: ${order.status}"
        tvDescription.text = "Descripción: ${order.description}"
        
        // Set total amount
        val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        tvTotalAmount.text = "Total: ${format.format(order.totalAmount)}"
        
        // Close button
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        // Edit button
        btnEdit.setOnClickListener {
            dialog.dismiss()
            showEditOrderDialog(order)
        }
        
        // Delete button
        btnDelete.setOnClickListener {
            showLoading(true)
            confirmDeleteOrder(order, dialog)
        }
        
        dialog.show()
        // Set dialog width
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    
    private fun showEditOrderDialog(order: Order) {
        val builder = AlertDialog.Builder(this, R.style.RoundedCornerDialog)
        
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_edit_order, null)
        builder.setView(dialogLayout)
        
        val dialog = builder.create()
        
        val etCustomerName = dialogLayout.findViewById<EditText>(R.id.etCustomerName)
        val etPhone = dialogLayout.findViewById<EditText>(R.id.etPhone)
        val etAddress = dialogLayout.findViewById<EditText>(R.id.etAddress)
        val spinnerStatus = dialogLayout.findViewById<Spinner>(R.id.spinnerStatus)
        val etDescription = dialogLayout.findViewById<EditText>(R.id.etDescription)
        val etTotalAmount = dialogLayout.findViewById<EditText>(R.id.etTotalAmount)
        val btnClose = dialogLayout.findViewById<ImageButton>(R.id.btnClose)
        val btnSaveOrder = dialogLayout.findViewById<Button>(R.id.btnSaveOrder)
        
        // Populate form with order data
        etCustomerName.setText(order.customerName)
        etPhone.setText(order.phone)
        etAddress.setText(order.address)
        etDescription.setText(order.description)
        etTotalAmount.setText(order.totalAmount.toString())
        
        // Setup status spinner
        val statusOptions = arrayOf("Pendiente", "En Proceso", "Completado", "Cancelado")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = statusAdapter
        
        // Set current status
        val statusPosition = statusOptions.indexOf(order.status)
        if (statusPosition >= 0) {
            spinnerStatus.setSelection(statusPosition)
        }
        
        // Close button
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        // Save order
        btnSaveOrder.setOnClickListener {
            showLoading(true)
            
            val customerName = etCustomerName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val totalAmountStr = etTotalAmount.text.toString().trim()
            val status = statusOptions[spinnerStatus.selectedItemPosition]
            
            // Validate inputs
            if (customerName.isEmpty()) {
                Toast.makeText(this, "Ingrese el nombre del cliente", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            if (phone.isEmpty()) {
                Toast.makeText(this, "Ingrese el teléfono", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            if (address.isEmpty()) {
                Toast.makeText(this, "Ingrese la dirección de entrega", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            if (description.isEmpty()) {
                Toast.makeText(this, "Ingrese una descripción del pedido", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            if (totalAmountStr.isEmpty()) {
                Toast.makeText(this, "Ingrese el monto total", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            val totalAmount = totalAmountStr.toDoubleOrNull()
            if (totalAmount == null || totalAmount <= 0) {
                Toast.makeText(this, "El monto total debe ser un número positivo", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            // Update the order
            val updatedOrder = Order(
                id = order.id,
                customerName = customerName,
                phone = phone,
                address = address,
                description = description,
                totalAmount = totalAmount,
                orderDate = order.orderDate,
                status = status
            )
            
            database.child("orders").child(order.id).setValue(updatedOrder)
                .addOnSuccessListener {
                    Toast.makeText(this, "Pedido actualizado exitosamente", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al actualizar pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
        }
        
        dialog.show()
        // Set dialog width
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    
    private fun confirmDeleteOrder(order: Order, parentDialog: AlertDialog) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar pedido")
            .setMessage("¿Está seguro que desea eliminar este pedido? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteOrder(order)
                parentDialog.dismiss()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                showLoading(false)
            }
            .show()
    }
    
    private fun deleteOrder(order: Order) {
        showLoading(true)
        database.child("orders").child(order.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Pedido eliminado exitosamente", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
    
    /**
     * Muestra el diálogo de búsqueda de pedidos
     */
    private fun showSearchOrderDialog() {
        val builder = AlertDialog.Builder(this, R.style.RoundedCornerDialog)
        
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_search_order, null)
        builder.setView(dialogLayout)
        
        val dialog = builder.create()
        
        val etCustomerName = dialogLayout.findViewById<EditText>(R.id.etCustomerName)
        val radioGroupStatus = dialogLayout.findViewById<RadioGroup>(R.id.radioGroupStatus)
        val btnSearchOrder = dialogLayout.findViewById<Button>(R.id.btnSearchOrder)
        val btnClose = dialogLayout.findViewById<ImageButton>(R.id.btnClose)
        
        // Configurar el botón de búsqueda
        btnSearchOrder.setOnClickListener {
            val customerName = etCustomerName.text.toString().trim()
            
            // Obtener el estado seleccionado
            val selectedRadioButtonId = radioGroupStatus.checkedRadioButtonId
            val status = when (selectedRadioButtonId) {
                R.id.radioPending -> "Pendiente"
                R.id.radioInProgress -> "En Proceso"
                R.id.radioCompleted -> "Completado"
                R.id.radioCancelled -> "Cancelado"
                else -> "" // Todos
            }
            
            // Realizar la búsqueda
            searchOrders(customerName, status)
            dialog.dismiss()
        }
        
        // Configurar el botón de cierre
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
        // Configurar ancho del diálogo
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun searchOrders(customerName: String, status: String) {
        // Mostrar indicador de carga
        showLoading(true)
        
        // Filtrar la lista de pedidos según los criterios
        filteredOrdersList.clear()

        val lowercaseCustomerName = customerName.lowercase()
        
        for (order in ordersList) {
            // Verificar si coincide con el nombre del cliente (si se proporcionó)
            val nameMatches = customerName.isEmpty() || 
                              order.customerName.lowercase().contains(lowercaseCustomerName)
            
            // Verificar si coincide con el estado (si se proporcionó)
            val statusMatches = status.isEmpty() || order.status == status
            
            // Si coincide con ambos criterios, añadir a la lista filtrada
            if (nameMatches && statusMatches) {
                filteredOrdersList.add(order)
            }
        }
        
        // Ordenar por fecha descendente (más recientes primero)
        filteredOrdersList.sortByDescending { it.orderDate }
        
        // Actualizar el adaptador con los pedidos filtrados
        orderAdapter.updateOrders(filteredOrdersList)
        
        // Actualizar estado de búsqueda
        isSearchMode = true
        
        // Mostrar mensaje si no hay resultados
        if (filteredOrdersList.isEmpty()) {
            val statusText = if (status.isNotEmpty()) " con estado '$status'" else ""
            val nameText = if (customerName.isNotEmpty()) " para cliente '$customerName'" else ""
            val message = "No se encontraron pedidos${nameText}${statusText}"
            
            textEmptyOrders.text = message
            textEmptyOrders.visibility = View.VISIBLE
            recyclerOrders.visibility = View.GONE
            
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } else {
            textEmptyOrders.visibility = View.GONE
            recyclerOrders.visibility = View.VISIBLE
            
            val statusText = if (status.isNotEmpty()) " con estado '$status'" else ""
            val nameText = if (customerName.isNotEmpty()) " para cliente '$customerName'" else ""
            val message = "${filteredOrdersList.size} pedidos encontrados${nameText}${statusText}"
            
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        // Añadir un botón para volver a todos los pedidos
        buttonAdd.text = "Mostrar Todos"
        buttonAdd.setOnClickListener {
            resetSearch()
        }
        
        // Ocultar indicador de carga
        showLoading(false)
    }
    
    /**
     * Restablece la búsqueda y muestra todos los pedidos
     */
    private fun resetSearch() {
        // Restablecer el modo de búsqueda
        isSearchMode = false
        
        // Mostrar todos los pedidos
        orderAdapter.updateOrders(ordersList)
        
        // Actualizar la visibilidad según si hay pedidos
        if (ordersList.isEmpty()) {
            textEmptyOrders.text = "No hay pedidos disponibles"
            textEmptyOrders.visibility = View.VISIBLE
            recyclerOrders.visibility = View.GONE
        } else {
            textEmptyOrders.visibility = View.GONE
            recyclerOrders.visibility = View.VISIBLE
        }
        
        // Restablecer el texto y listener del botón de agregar
        buttonAdd.text = "Agregar +"
        buttonAdd.setOnClickListener {
            showAddOrderDialog()
        }
        
        Toast.makeText(this, "Mostrando todos los pedidos", Toast.LENGTH_SHORT).show()
    }
}