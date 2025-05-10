package com.example.projectofinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class CalculatorActivity : AppCompatActivity() {
    
    // UI Elements
    private lateinit var productNameInput: EditText
    private lateinit var materialNameInput: EditText
    private lateinit var materialCostInput: EditText
    private lateinit var addMaterialButton: ImageButton
    private lateinit var materialsRecyclerView: RecyclerView
    private lateinit var hoursInput: EditText
    private lateinit var hourlyRateInput: EditText
    private lateinit var shippingCostInput: EditText
    private lateinit var discountInput: EditText
    private lateinit var calculateButton: Button
    private lateinit var totalPriceResult: TextView
    
    // Lista de materiales
    private val materialsList = mutableListOf<Material>()
    private lateinit var materialsAdapter: MaterialsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)
        
        // Inicializar vistas
        initViews()
        
        // Inicializar RecyclerView para materiales
        setupRecyclerView()
        
        // Configurar listeners
        setupListeners()
        
        // Configurar botones de navegación
        setupNavigationButtons()
    }
    
    private fun initViews() {
        productNameInput = findViewById(R.id.productNameInput)
        materialNameInput = findViewById(R.id.materialNameInput)
        materialCostInput = findViewById(R.id.materialCostInput)
        addMaterialButton = findViewById(R.id.addMaterialButton)
        materialsRecyclerView = findViewById(R.id.materialsRecyclerView)
        hoursInput = findViewById(R.id.hoursInput)
        hourlyRateInput = findViewById(R.id.hourlyRateInput)
        shippingCostInput = findViewById(R.id.shippingCostInput)
        discountInput = findViewById(R.id.discountInput)
        calculateButton = findViewById(R.id.calculateButton)
        totalPriceResult = findViewById(R.id.totalPriceResult)
    }
    
    private fun setupRecyclerView() {
        materialsAdapter = MaterialsAdapter(materialsList) { position ->
            // Manejo de eliminación de material
            materialsList.removeAt(position)
            materialsAdapter.notifyItemRemoved(position)
            updateMaterialsVisibility()
        }
        
        materialsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CalculatorActivity)
            adapter = materialsAdapter
        }
    }
    
    private fun setupListeners() {
        // Botón para añadir material
        addMaterialButton.setOnClickListener {
            addMaterial()
        }
        
        // Botón para calcular precio
        calculateButton.setOnClickListener {
            calculatePrice()
        }
    }
    
    private fun addMaterial() {
        val materialName = materialNameInput.text.toString().trim()
        val materialCostText = materialCostInput.text.toString().trim()
        
        if (materialName.isEmpty() || materialCostText.isEmpty()) {
            Toast.makeText(this, "Por favor ingresa nombre y costo del material", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            val materialCost = materialCostText.toDouble()
            val material = Material(materialName, materialCost)
            materialsList.add(material)
            materialsAdapter.notifyItemInserted(materialsList.size - 1)
            
            // Limpiar campos
            materialNameInput.text.clear()
            materialCostInput.text.clear()
            materialNameInput.requestFocus()
            
            // Mostrar RecyclerView si es necesario
            updateMaterialsVisibility()
            
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Por favor ingresa un costo válido", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateMaterialsVisibility() {
        if (materialsList.isEmpty()) {
            materialsRecyclerView.visibility = View.GONE
        } else {
            materialsRecyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun calculatePrice() {
        try {
            // Costo total de materiales
            val materialsCost = materialsList.sumOf { it.cost }
            
            // Costo de mano de obra
            val hoursText = hoursInput.text.toString().trim()
            val hourlyRateText = hourlyRateInput.text.toString().trim()
            
            val laborCost = if (hoursText.isNotEmpty() && hourlyRateText.isNotEmpty()) {
                hoursText.toDouble() * hourlyRateText.toDouble()
            } else {
                0.0
            }
            
            // Costo de envío
            val shippingText = shippingCostInput.text.toString().trim()
            val shippingCost = if (shippingText.isNotEmpty()) {
                shippingText.toDouble()
            } else {
                0.0
            }
            
            // Descuento
            val discountText = discountInput.text.toString().trim()
            val discount = if (discountText.isNotEmpty()) {
                discountText.toDouble()
            } else {
                0.0
            }
            
            // Calcular precio total
            val subtotal = materialsCost + laborCost + shippingCost
            val discountAmount = subtotal * discount
            val total = subtotal - discountAmount
            
            // Formatear y mostrar resultado
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            totalPriceResult.text = currencyFormat.format(total)
            totalPriceResult.visibility = View.VISIBLE
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error al calcular: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Clase de datos para Material
    data class Material(val name: String, val cost: Double)
    
    private fun setupNavigationButtons() {
        // Botón Inicio
        findViewById<ImageButton>(R.id.homeButton).setOnClickListener {
            val intent = Intent(this, InventoryActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Botón Pedidos
        findViewById<ImageButton>(R.id.orderButton).setOnClickListener {
            val intent = Intent(this, OrderActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Botón Recordatorios
        findViewById<ImageButton>(R.id.recordsButton).setOnClickListener {
            val intent = Intent(this, ReminderActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Botón Información
        findViewById<ImageButton>(R.id.infoButton).setOnClickListener {
            val intent = Intent(this, InfoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}