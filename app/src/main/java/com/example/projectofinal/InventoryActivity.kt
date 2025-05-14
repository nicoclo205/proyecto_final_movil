package com.example.projectofinal

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectofinal.adapter.ProductAdapter
import com.example.projectofinal.model.Product
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class InventoryActivity : AppCompatActivity() {

    private lateinit var buttonAdd: Button
    private lateinit var buttonSearch: Button
    private lateinit var recyclerProducts: RecyclerView
    private lateinit var textEmptyProducts: TextView
    private lateinit var productAdapter: ProductAdapter
    private val productsList = mutableListOf<Product>()
    private val filteredProductsList = mutableListOf<Product>() // Lista para productos filtrados
    private var selectedImageUri: Uri? = null
    private var currentImageButton: Button? = null  // Referencia al botón de imagen actual
    private val storageRef = FirebaseStorage.getInstance().reference
    private val database = FirebaseDatabase.getInstance().reference
    
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingBackground: View
    
    // Variable para rastrear si estamos en modo búsqueda
    private var isSearchMode = false
    
    // Activity result launcher for image selection
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                // Actualizar la vista previa de la imagen en el botón actual
                currentImageButton?.let { button ->
                    Glide.with(this)
                        .asBitmap()
                        .load(uri)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                button.background = BitmapDrawable(resources, resource)
                                button.text = ""
                            }
                            
                            override fun onLoadCleared(placeholder: Drawable?) {
                                // No hacemos nada aquí
                            }
                            
                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                // Mantener texto del botón en caso de error
                                Toast.makeText(this@InventoryActivity, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
                            }
                        })
                }
                Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        buttonAdd = findViewById(R.id.buttonAdd)
        buttonSearch = findViewById(R.id.buttonSearch)
        recyclerProducts = findViewById(R.id.recyclerProducts)
        textEmptyProducts = findViewById(R.id.textEmptyProducts)
        
        progressBar = findViewById(R.id.progressBar)
        loadingBackground = findViewById(R.id.loadingBackground)
        
        // Asegurar que la carga inicie de forma oculta
        progressBar.visibility = View.GONE
        loadingBackground.visibility = View.GONE
        
        // Setup navigation buttons if they exist
        val orderButton = findViewById<ImageButton>(R.id.orderButton)
        orderButton?.setOnClickListener {
            startActivity(Intent(this, OrderActivity::class.java))
            finish()
        }

        val calulatorButton = findViewById<ImageButton>(R.id.calculatorButton)
        calulatorButton?.setOnClickListener{
            startActivity(Intent(this, CalculatorActivity::class.java))
            finish()
        }

        val reminderButton = findViewById<ImageButton>(R.id.recordsButton)
        reminderButton?.setOnClickListener{
            startActivity(Intent(this, ReminderActivity::class.java))
            finish()
        }

        val infoButton = findViewById<ImageButton>(R.id.infoButton)
        infoButton?.setOnClickListener{
            startActivity(Intent(this, InfoActivity::class.java))
            finish()
        }
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Load products from Firebase
        loadProducts()
        
        // Configurar el botón de búsqueda
        buttonSearch.setOnClickListener {
            showSearchDialog()
        }

        buttonAdd.setOnClickListener{
            val builder = AlertDialog.Builder(this, R.style.RoundedCornerDialog)

            val inflater = layoutInflater
            val dialogLayout = inflater.inflate(R.layout.dialog_add_inventory, null)
            builder.setView(dialogLayout)

            val dialog = builder.create()
            
            val btnAddImage = dialogLayout.findViewById<Button>(R.id.addImage)
            val etProductName = dialogLayout.findViewById<EditText>(R.id.etAddProduct)
            val etQuantity = dialogLayout.findViewById<EditText>(R.id.etAddNum)
            val etPrice = dialogLayout.findViewById<EditText>(R.id.etAddPrice)
            val btnAddProduct = dialogLayout.findViewById<Button>(R.id.buttonAddProduct)
            val btnClose = dialogLayout.findViewById<Button>(R.id.cancelButton)
            
            // manejo de imagenes
            btnAddImage.setOnClickListener {
                currentImageButton = btnAddImage  // Guardar referencia al botón actual
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                getContent.launch(intent)
            }
            
            // manejo agregación  productos
            btnAddProduct.setOnClickListener {
                // Mostrar carga
                showLoading(true)
                val name = etProductName.text.toString().trim()
                val quantityStr = etQuantity.text.toString().trim()
                val priceStr = etPrice.text.toString().trim()
                
                // validaciones
                if (name.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty()) {
                    Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val quantity = quantityStr.toIntOrNull()
                if (quantity == null || quantity <= 0) {
                    Toast.makeText(this, "La cantidad debe ser un número positivo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val price = priceStr.toDoubleOrNull()
                if (price == null || price <= 0) {
                    Toast.makeText(this, "El precio debe ser un número positivo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // Upload image first if selected, then add product data
                if (selectedImageUri != null) {
                    uploadImageAndAddProduct(name, quantity, price, dialog)
                } else {
                    // Add product without image
                    addProductToDatabase(name, quantity, price, "", dialog)
                }
            }

            btnClose.setOnClickListener{
                currentImageButton = null  // Limpiar la referencia al botón
                dialog.dismiss()
            }

            dialog.show()
        // Configurar ancho del diálogo
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            // Configurar ancho del diálogo
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
    
    private fun uploadImageAndAddProduct(name: String, quantity: Int, price: Double, dialog: AlertDialog) {
        val productId = database.child("products").push().key ?: return
        val imageRef = storageRef.child("product_images/$productId.jpg")
        
        // Mostrar progress bar en el diálogo
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        val loadingBackground = dialog.findViewById<View>(R.id.loadingBackground)
        if (progressBar != null) {
            progressBar.visibility = View.VISIBLE
        }
        if (loadingBackground != null) {
            loadingBackground.visibility = View.VISIBLE
        }
        
        val uploadTask = selectedImageUri?.let { imageRef.putFile(it) }
        uploadTask?.addOnSuccessListener {
            // Get download URL
            imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                addProductToDatabase(name, quantity, price, downloadUrl.toString(), dialog)
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener URL de imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }?.addOnFailureListener { e ->
            Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(productsList)
        recyclerProducts.layoutManager = LinearLayoutManager(this)
        recyclerProducts.adapter = productAdapter
        
        // Setup click listener for item editing
        productAdapter.setOnProductClickListener(object : ProductAdapter.OnProductClickListener {
            override fun onProductClick(product: Product, position: Int) {
                showEditProductDialog(product)
            }
        })
    }
    
    //función para mostrar u ocultar el indicador de carga
    private fun showLoading(isLoading: Boolean){
        if(isLoading){
            progressBar.visibility = View.VISIBLE
            loadingBackground.visibility = View.VISIBLE
            
            buttonAdd.isEnabled = false
        } else {
            progressBar.visibility = View.GONE
            loadingBackground.visibility = View.GONE
            
            buttonAdd.isEnabled = true
        }
    }
    
    private fun loadProducts() {
        // Mostrar indicador de carga
        showLoading(true)
        database.child("products").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productsList.clear()
                
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let { productsList.add(it) }
                }
                
                // Si estamos en modo búsqueda, mantener los productos filtrados
                if (isSearchMode) {
                    // No actualizamos el adaptador ni cambiamos la visibilidad
                } else {
                    // En modo normal, mostrar todos los productos
                    productAdapter.updateProducts(productsList)
                    
                    // Show empty state if needed
                    if (productsList.isEmpty()) {
                        textEmptyProducts.visibility = View.VISIBLE
                        recyclerProducts.visibility = View.GONE
                    } else {
                        textEmptyProducts.visibility = View.GONE
                        recyclerProducts.visibility = View.VISIBLE
                    }
                }
                
                // Ocultar indicador de carga
                showLoading(false)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@InventoryActivity, "Error al cargar productos: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
                // Ocultar indicador de carga en caso de error
                showLoading(false)
            }
        })
    }
    
    private fun addProductToDatabase(name: String, quantity: Int, price: Double, imageUrl: String, dialog: AlertDialog) {
        val productId = database.child("products").push().key ?: return
        val product = Product(productId, name, quantity, price, imageUrl)
        
        // Mostrar progress bar en el diálogo si no está visible
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        val loadingBackground = dialog.findViewById<View>(R.id.loadingBackground)
        if (progressBar != null) {
            if (progressBar.visibility != View.VISIBLE) {
                progressBar.visibility = View.VISIBLE
                if (loadingBackground != null) {
                    loadingBackground.visibility = View.VISIBLE
                }
            }
        }
        
        database.child("products").child(productId).setValue(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto agregado exitosamente", Toast.LENGTH_SHORT).show()
                selectedImageUri = null
                currentImageButton = null  // Limpiar la referencia al botón
                // Ocultar progress bar
                if (progressBar != null) {
                    progressBar.visibility = View.GONE
                }
                if (loadingBackground != null) {
                    loadingBackground.visibility = View.GONE
                }
                dialog.dismiss()

            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al agregar producto: ${e.message}", Toast.LENGTH_SHORT).show()
                // Ocultar progress bar
                if (progressBar != null) {
                    progressBar.visibility = View.GONE
                }
                if (loadingBackground != null) {
                    loadingBackground.visibility = View.GONE
                }
            }
    }
    
    private fun showEditProductDialog(product: Product) {
        val builder = AlertDialog.Builder(this, R.style.RoundedCornerDialog)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_edit_inventory, null)
        builder.setView(dialogLayout)
        
        val dialog = builder.create()
        
        val btnEditImage = dialogLayout.findViewById<Button>(R.id.editImage)
        val etProductName = dialogLayout.findViewById<EditText>(R.id.etEditProduct)
        val etQuantity = dialogLayout.findViewById<EditText>(R.id.etEditNum)
        val etPrice = dialogLayout.findViewById<EditText>(R.id.etEditPrice)
        val btnUpdateProduct = dialogLayout.findViewById<Button>(R.id.editButton)  // Cambio a ImageButton
        val btnDeleteProduct = dialogLayout.findViewById<Button>(R.id.deleteButton)  // Cambio a ImageButton
        val btnClose = dialogLayout.findViewById<Button>(R.id.cancelButton)
        
        // Fill the form with product data
        etProductName.setText(product.name)
        etQuantity.setText(product.quantity.toString())
        etPrice.setText(product.price.toString())
        
        // Load product image if available
        if (product.imageUrl.isNotEmpty()) {
            try {
                // Usar Glide
                Glide.with(this)
                    .asBitmap()
                    .load(product.imageUrl)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            btnEditImage.background = BitmapDrawable(resources, resource)
                            btnEditImage.text = ""
                        }
                        
                        override fun onLoadCleared(placeholder: Drawable?) {
                            // No hacemos nada aquí
                        }
                        
                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            // Mantener background y texto predeterminados
                        }
                    })
            } catch (e: Exception) {
                // Ignorar errores de carga de imágenes
            }
        }
        
        // Setup image selection
        btnEditImage.setOnClickListener {
            currentImageButton = btnEditImage  // Guardar referencia al botón actual
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getContent.launch(intent)
        }
        
        // Handle update
        btnUpdateProduct.setOnClickListener {
            showLoading(true)
            val name = etProductName.text.toString().trim()
            val quantityStr = etQuantity.text.toString().trim()
            val priceStr = etPrice.text.toString().trim()
            
            // Validate inputs
            if (name.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            val quantity = quantityStr.toIntOrNull()
            if (quantity == null || quantity <= 0) {
                Toast.makeText(this, "La cantidad debe ser un número positivo", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            val price = priceStr.toDoubleOrNull()
            if (price == null || price <= 0) {
                Toast.makeText(this, "El precio debe ser un número positivo", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return@setOnClickListener
            }
            
            // If new image selected, upload it first
            if (selectedImageUri != null) {
                updateProductWithImage(product.id, name, quantity, price, dialog)
            } else {
                // Update product without changing the image
                updateProduct(product.id, name, quantity, price, product.imageUrl, dialog)
            }
        }
        
        // manejo borrar producto
        btnDeleteProduct.setOnClickListener {
            showLoading(true)
            // Show confirmation dialog
            AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Está seguro que desea eliminar este producto?")
                .setPositiveButton("Sí") { _, _ ->
                    deleteProduct(product.id, product.imageUrl, dialog)
                }
                .setNegativeButton("No") { _, _ ->
                    showLoading(false)
                }
                .show()
        }
        
        btnClose.setOnClickListener{
            currentImageButton = null  // Limpiar la referencia al botón
            dialog.dismiss()
        }
        
        dialog.show()
        // Configurar ancho del diálogo
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        // Configurar ancho del diálogo
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    
    private fun updateProductWithImage(productId: String, name: String, quantity: Int, price: Double, dialog: AlertDialog) {
        val imageRef = storageRef.child("product_images/$productId.jpg")
        
        val uploadTask = selectedImageUri?.let { imageRef.putFile(it) }
        uploadTask?.addOnSuccessListener {
            // Get download URL
            imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                updateProduct(productId, name, quantity, price, downloadUrl.toString(), dialog)
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener URL de imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                // Ocultar progress bar en caso de error
                progressBar.visibility = View.GONE
                loadingBackground.visibility = View.GONE
            }
        }?.addOnFailureListener { e ->
            Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            // Ocultar progress bar en caso de error
            progressBar.visibility = View.GONE
            loadingBackground.visibility = View.GONE
        }
    }
    
    private fun updateProduct(productId: String, name: String, quantity: Int, price: Double, imageUrl: String, dialog: AlertDialog) {
        val productUpdates = mapOf(
            "name" to name,
            "quantity" to quantity,
            "price" to price,
            "imageUrl" to imageUrl
        )
        
        // Mostrar progress bar en el diálogo si no está visible
        val progressBar = dialog.findViewById<ProgressBar>(R.id.progressBar)
        val loadingBackground = dialog.findViewById<View>(R.id.loadingBackground)
        if (progressBar != null) {
            if (progressBar.visibility != View.VISIBLE) {
                progressBar.visibility = View.VISIBLE
                if (loadingBackground != null) {
                    loadingBackground.visibility = View.VISIBLE
                }
            }
        }
        
        database.child("products").child(productId).updateChildren(productUpdates)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto actualizado exitosamente", Toast.LENGTH_SHORT).show()
                selectedImageUri = null
                currentImageButton = null  // Limpiar la referencia al botón
                // Ocultar progress bar
                if (progressBar != null) {
                    progressBar.visibility = View.GONE
                }
                if (loadingBackground != null) {
                    loadingBackground.visibility = View.GONE
                }
                dialog.dismiss()
                // Products will be refreshed automatically by the ValueEventListener
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar producto: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
    
    private fun deleteProduct(productId: String, imageUrl: String, dialog: AlertDialog) {
        // First delete from database
        database.child("products").child(productId).removeValue()
            .addOnSuccessListener {
                // If product has an image, delete it from storage too
                if (imageUrl.isNotEmpty()) {
                    try {
                        val imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                        imageRef.delete().addOnSuccessListener {
                            Toast.makeText(this, "Producto eliminado exitosamente", Toast.LENGTH_SHORT).show()
                            showLoading(false)
                            dialog.dismiss()
                        }.addOnFailureListener { e ->
                            // Even if image deletion fails, we consider it a success since the product data is gone
                            Toast.makeText(this, "Producto eliminado. Error al eliminar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                            showLoading(false)
                            dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        // If we can't get a reference to the image, just consider it a success
                        Toast.makeText(this, "Producto eliminado exitosamente", Toast.LENGTH_SHORT).show()
                        showLoading(false)
                        dialog.dismiss()
                    }
                } else {
                    Toast.makeText(this, "Producto eliminado exitosamente", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    dialog.dismiss()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar producto: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }
    
    /**
     * Muestra el diálogo de búsqueda de productos
     */
    private fun showSearchDialog() {
        val builder = AlertDialog.Builder(this, R.style.RoundedCornerDialog)
        
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_search_product, null)
        builder.setView(dialogLayout)
        
        val dialog = builder.create()
        
        val etProductName = dialogLayout.findViewById<EditText>(R.id.etProductName)
        val btnSearchProduct = dialogLayout.findViewById<Button>(R.id.btnSearchProduct)
        val btnClose = dialogLayout.findViewById<ImageButton>(R.id.btnClose)
        
        // Configurar el botón de búsqueda
        btnSearchProduct.setOnClickListener {
            val searchQuery = etProductName.text.toString().trim()
            
            if (searchQuery.isEmpty()) {
                Toast.makeText(this, "Ingrese un nombre para buscar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Realizar la búsqueda
            searchProducts(searchQuery)
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

    private fun searchProducts(query: String) {
        // Mostrar indicador de carga
        showLoading(true)
        
        // Filtrar la lista de productos según la consulta
        filteredProductsList.clear()
        
        // Pasar a minúsculas para una comparación sin distinción entre mayúsculas y minúsculas
        val lowercaseQuery = query.lowercase()
        
        for (product in productsList) {
            if (product.name.lowercase().contains(lowercaseQuery)) {
                filteredProductsList.add(product)
            }
        }
        
        // Actualizar el adaptador con los productos filtrados
        productAdapter.updateProducts(filteredProductsList)
        
        // Actualizar estado de búsqueda
        isSearchMode = true
        
        // Mostrar mensaje si no hay resultados
        if (filteredProductsList.isEmpty()) {
            textEmptyProducts.text = "No se encontraron productos que coincidan con '$query'"
            textEmptyProducts.visibility = View.VISIBLE
            recyclerProducts.visibility = View.GONE
            
            // Añadir un botón para volver a todos los productos
            Toast.makeText(this, "No se encontraron productos que coincidan con '$query'", Toast.LENGTH_SHORT).show()
        } else {
            textEmptyProducts.visibility = View.GONE
            recyclerProducts.visibility = View.VISIBLE
            
            Toast.makeText(this, "${filteredProductsList.size} productos encontrados", Toast.LENGTH_SHORT).show()
        }
        
        // Añadir un botón para volver a todos los productos
        buttonAdd.text = "Todos"
        buttonAdd.setOnClickListener {
            resetSearch()
        }
        
        // Ocultar indicador de carga
        showLoading(false)
    }

    private fun resetSearch() {
        // Restablecer el modo de búsqueda
        isSearchMode = false
        
        // Mostrar todos los productos
        productAdapter.updateProducts(productsList)
        
        // Actualizar la visibilidad según si hay productos
        if (productsList.isEmpty()) {
            textEmptyProducts.text = "No hay productos en inventario"
            textEmptyProducts.visibility = View.VISIBLE
            recyclerProducts.visibility = View.GONE
        } else {
            textEmptyProducts.visibility = View.GONE
            recyclerProducts.visibility = View.VISIBLE
        }
        
        // Restablecer el texto y listener del botón de agregar
        buttonAdd.text = "Agregar +"
        buttonAdd.setOnClickListener {
            val builder = AlertDialog.Builder(this, R.style.RoundedCornerDialog)

            val inflater = layoutInflater
            val dialogLayout = inflater.inflate(R.layout.dialog_add_inventory, null)
            builder.setView(dialogLayout)

            val dialog = builder.create()
            
            val btnAddImage = dialogLayout.findViewById<Button>(R.id.addImage)
            val etProductName = dialogLayout.findViewById<EditText>(R.id.etAddProduct)
            val etQuantity = dialogLayout.findViewById<EditText>(R.id.etAddNum)
            val etPrice = dialogLayout.findViewById<EditText>(R.id.etAddPrice)
            val btnAddProduct = dialogLayout.findViewById<Button>(R.id.buttonAddProduct)
            val btnClose = dialogLayout.findViewById<Button>(R.id.cancelButton)
            
            // manejo de imagenes
            btnAddImage.setOnClickListener {
                currentImageButton = btnAddImage  // Guardar referencia al botón actual
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                getContent.launch(intent)
            }
            
            // manejo agregación  productos
            btnAddProduct.setOnClickListener {
                // Mostrar carga
                showLoading(true)
                val name = etProductName.text.toString().trim()
                val quantityStr = etQuantity.text.toString().trim()
                val priceStr = etPrice.text.toString().trim()
                
                // validaciones
                if (name.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty()) {
                    Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val quantity = quantityStr.toIntOrNull()
                if (quantity == null || quantity <= 0) {
                    Toast.makeText(this, "La cantidad debe ser un número positivo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val price = priceStr.toDoubleOrNull()
                if (price == null || price <= 0) {
                    Toast.makeText(this, "El precio debe ser un número positivo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // Upload image first if selected, then add product data
                if (selectedImageUri != null) {
                    uploadImageAndAddProduct(name, quantity, price, dialog)
                } else {
                    // Add product without image
                    addProductToDatabase(name, quantity, price, "", dialog)
                }
            }

            btnClose.setOnClickListener{
                currentImageButton = null  // Limpiar la referencia al botón
                dialog.dismiss()
            }

            dialog.show()
        // Configurar ancho del diálogo
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            // Configurar ancho del diálogo
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        
        Toast.makeText(this, "Mostrando todos los productos", Toast.LENGTH_SHORT).show()
    }
}