package com.example.projectofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectofinal.adapter.OrderAdapter
import com.example.projectofinal.adapter.OrderProductAdapter
import com.example.projectofinal.model.Order
import com.example.projectofinal.model.OrderProduct
import com.example.projectofinal.model.Product
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
    private val productsList = mutableListOf<Product>()
    
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
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Load products for later use in order creation
        loadProducts()
        
        // Load orders from Firebase
        loadOrders()
        
        // Setup button clicks
        setupButtonListeners()
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
        database.child("products").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productsList.clear()
                
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let { productsList.add(it) }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@OrderActivity, "Error al cargar productos: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun loadOrders() {
        database.child("orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ordersList.clear()
                
                for (orderSnapshot in snapshot.children) {
                    val order = orderSnapshot.getValue(Order::class.java)
                    order?.let { ordersList.add(it) }
                }
                
                // Update adapter
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
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@OrderActivity, "Error al cargar pedidos: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun setupButtonListeners() {
        // Add new order
        buttonAdd.setOnClickListener {
            showAddOrderDialog()
        }
        
        // Search orders (you could implement this functionality)
        buttonSearch.setOnClickListener {
            Toast.makeText(this, "Funcionalidad de búsqueda en desarrollo", Toast.LENGTH_SHORT).show()
        }
        
        // Navigation buttons
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
        val recyclerOrderProducts = dialogLayout.findViewById<RecyclerView>(R.id.recyclerOrderProducts)
        val btnAddProduct = dialogLayout.findViewById<Button>(R.id.btnAddProduct)
        val tvTotalAmount = dialogLayout.findViewById<TextView>(R.id.tvTotalAmount)
        val btnClose = dialogLayout.findViewById<ImageButton>(R.id.btnClose)
        val btnSaveOrder = dialogLayout.findViewById<Button>(R.id.btnSaveOrder)
        
        // Setup RecyclerView for order products
        val orderProductsList = mutableListOf<OrderProduct>()
        val orderProductAdapter = OrderProductAdapter(orderProductsList)
        recyclerOrderProducts.layoutManager = LinearLayoutManager(this)
        recyclerOrderProducts.adapter = orderProductAdapter
        
        // Handle removing products from the order
        orderProductAdapter.setOnOrderProductClickListener(object : OrderProductAdapter.OnOrderProductClickListener {
            override fun onRemoveProductClick(orderProduct: OrderProduct, position: Int) {
                orderProductAdapter.removeProduct(position)
                // Update total amount
                val totalAmount = orderProductAdapter.calculateTotal()
                val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
                tvTotalAmount.text = "Total: ${format.format(totalAmount)}"
            }
        })
        
        // Add product to order
        btnAddProduct.setOnClickListener {
            if (productsList.isEmpty()) {
                Toast.makeText(this, "No hay productos disponibles", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            showSelectProductDialog(orderProductAdapter, tvTotalAmount)
        }
        
        // Close button
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        // Save order
        btnSaveOrder.setOnClickListener {
            val customerName = etCustomerName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val address = etAddress.text.toString().trim()
            
            // Validate inputs
            if (customerName.isEmpty()) {
                Toast.makeText(this, "Ingrese el nombre del cliente", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (phone.isEmpty()) {
                Toast.makeText(this, "Ingrese el teléfono", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (address.isEmpty()) {
                Toast.makeText(this, "Ingrese la dirección de entrega", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (orderProductAdapter.getOrderProducts().isEmpty()) {
                Toast.makeText(this, "Agregue al menos un producto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Create and save the order
            val orderId = database.child("orders").push().key ?: return@setOnClickListener
            val totalAmount = orderProductAdapter.calculateTotal()
            
            val order = Order(
                id = orderId,
                customerName = customerName,
                phone = phone,
                address = address,
                products = orderProductAdapter.getOrderProducts(),
                totalAmount = totalAmount,
                orderDate = Date().time,
                status = "Pendiente"
            )
            
            database.child("orders").child(orderId).setValue(order)
                .addOnSuccessListener {
                    Toast.makeText(this, "Pedido guardado exitosamente", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    
                    // Optionally update product inventory quantities
                    updateInventoryQuantities(order.products)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar pedido: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        
        dialog.show()
        // Set dialog width
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    
    private fun showSelectProductDialog(orderProductAdapter: OrderProductAdapter, tvTotalAmount: TextView) {
        val builder = AlertDialog.Builder(this, R.style.RoundedCornerDialog)
        
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_select_product, null)
        builder.setView(dialogLayout)
        
        val dialog = builder.create()
        
        val spinnerProducts = dialogLayout.findViewById<Spinner>(R.id.spinnerProducts)
        val etQuantity = dialogLayout.findViewById<EditText>(R.id.etQuantity)
        val tvAvailableQuantity = dialogLayout.findViewById<TextView>(R.id.tvAvailableQuantity)
        val tvProductPrice = dialogLayout.findViewById<TextView>(R.id.tvProductPrice)
        val tvSubtotal = dialogLayout.findViewById<TextView>(R.id.tvSubtotal)
        val btnCloseProduct = dialogLayout.findViewById<ImageButton>(R.id.btnCloseProduct)
        val btnAddToOrder = dialogLayout.findViewById<Button>(R.id.btnAddToOrder)
        
        // Setup products spinner
        val productNames = productsList.map { it.name }.toMutableList()
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, productNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProducts.adapter = spinnerAdapter
        
        var selectedProduct: Product? = null
        
        // Handle product selection
        spinnerProducts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedProduct = productsList[position]
                
                // Update available quantity
                tvAvailableQuantity.text = "Disponible: ${selectedProduct?.quantity ?: 0}"
                
                // Update price
                val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
                tvProductPrice.text = "Precio: ${format.format(selectedProduct?.price ?: 0.0)}"
                
                // Reset quantity and subtotal
                etQuantity.setText("1")
                calculateSubtotal(selectedProduct, etQuantity, tvSubtotal)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedProduct = null
                tvAvailableQuantity.text = "Disponible: 0"
                tvProductPrice.text = "Precio: $0.00"
            }
        }
        
        // Calculate subtotal when quantity changes
        etQuantity.setOnEditorActionListener { _, _, _ ->
            calculateSubtotal(selectedProduct, etQuantity, tvSubtotal)
            false
        }
        
        // Close button
        btnCloseProduct.setOnClickListener {
            dialog.dismiss()
        }
        
        // Add product to order
        btnAddToOrder.setOnClickListener {
            val product = selectedProduct ?: return@setOnClickListener
            
            val quantityStr = etQuantity.text.toString().trim()
            if (quantityStr.isEmpty()) {
                Toast.makeText(this, "Ingrese una cantidad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val quantity = quantityStr.toIntOrNull()
            if (quantity == null || quantity <= 0) {
                Toast.makeText(this, "La cantidad debe ser un número positivo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (quantity > product.quantity) {
                Toast.makeText(this, "No hay suficiente inventario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Check if product already exists in order
            val existingProducts = orderProductAdapter.getOrderProducts()
            val existingProductIndex = existingProducts.indexOfFirst { it.productId == product.id }
            
            if (existingProductIndex >= 0) {
                // Ask if user wants to update the quantity
                AlertDialog.Builder(this)
                    .setTitle("Producto ya en el pedido")
                    .setMessage("Este producto ya está en el pedido. ¿Desea actualizar la cantidad?")
                    .setPositiveButton("Sí") { _, _ ->
                        // Create a new order product with updated quantity
                        val existingProduct = existingProducts[existingProductIndex]
                        val newQuantity = existingProduct.quantity + quantity
                        
                        if (newQuantity > product.quantity) {
                            Toast.makeText(this, "No hay suficiente inventario para la cantidad total", 
                                Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        
                        // Remove old product and add updated one
                        orderProductAdapter.removeProduct(existingProductIndex)
                        
                        val orderProduct = OrderProduct(
                            productId = product.id,
                            productName = product.name,
                            quantity = newQuantity,
                            pricePerUnit = product.price
                        )
                        
                        orderProductAdapter.addProduct(orderProduct)
                        
                        // Update total
                        val totalAmount = orderProductAdapter.calculateTotal()
                        val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
                        tvTotalAmount.text = "Total: ${format.format(totalAmount)}"
                        
                        dialog.dismiss()
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                // Add new product to order
                val orderProduct = OrderProduct(
                    productId = product.id,
                    productName = product.name,
                    quantity = quantity,
                    pricePerUnit = product.price
                )
                
                orderProductAdapter.addProduct(orderProduct)
                
                // Update total
                val totalAmount = orderProductAdapter.calculateTotal()
                val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
                tvTotalAmount.text = "Total: ${format.format(totalAmount)}"
                
                dialog.dismiss()
            }
        }
        
        dialog.show()
        // Set dialog width
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    
    private fun calculateSubtotal(product: Product?, etQuantity: EditText, tvSubtotal: TextView) {
        val quantityStr = etQuantity.text.toString().trim()
        val quantity = quantityStr.toIntOrNull() ?: 0
        val price = product?.price ?: 0.0
        val subtotal = quantity * price
        
        val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        tvSubtotal.text = "Subtotal: ${format.format(subtotal)}"
    }
    
    private fun showOrderDetailsDialog(order: Order) {
        // You can implement this to show order details when an order is clicked
        Toast.makeText(this, "Pedido de ${order.customerName}", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateInventoryQuantities(orderProducts: List<OrderProduct>) {
        // Update inventory quantities based on order
        // This is optional and can be implemented based on your business logic
        for (orderProduct in orderProducts) {
            val productRef = database.child("products").child(orderProduct.productId)
            
            productRef.get().addOnSuccessListener { snapshot ->
                val product = snapshot.getValue(Product::class.java)
                product?.let {
                    val newQuantity = it.quantity - orderProduct.quantity
                    
                    if (newQuantity >= 0) {
                        productRef.child("quantity").setValue(newQuantity)
                    }
                }
            }
        }
    }
}