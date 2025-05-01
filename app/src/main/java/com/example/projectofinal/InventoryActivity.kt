package com.example.projectofinal

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
    private lateinit var recyclerProducts: RecyclerView
    private lateinit var textEmptyProducts: TextView
    private lateinit var productAdapter: ProductAdapter
    private val productsList = mutableListOf<Product>()
    private var selectedImageUri: Uri? = null
    private var currentImageButton: Button? = null  // Referencia al botón de imagen actual
    private val storageRef = FirebaseStorage.getInstance().reference
    private val database = FirebaseDatabase.getInstance().reference
    
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
        recyclerProducts = findViewById(R.id.recyclerProducts)
        textEmptyProducts = findViewById(R.id.textEmptyProducts)
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Load products from Firebase
        loadProducts()

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
    
    private fun loadProducts() {
        // Show loading or progress indicator if needed
        database.child("products").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productsList.clear()
                
                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let { productsList.add(it) }
                }
                
                // actualizar adapter
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
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@InventoryActivity, "Error al cargar productos: ${error.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun addProductToDatabase(name: String, quantity: Int, price: Double, imageUrl: String, dialog: AlertDialog) {
        val productId = database.child("products").push().key ?: return
        val product = Product(productId, name, quantity, price, imageUrl)
        
        database.child("products").child(productId).setValue(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto agregado exitosamente", Toast.LENGTH_SHORT).show()
                selectedImageUri = null
                currentImageButton = null  // Limpiar la referencia al botón
                dialog.dismiss()
                // Products will be refreshed automatically by the ValueEventListener
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al agregar producto: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val btnUpdateProduct = dialogLayout.findViewById<ImageButton>(R.id.editButton)  // Cambio a ImageButton
        val btnDeleteProduct = dialogLayout.findViewById<ImageButton>(R.id.deleteButton)  // Cambio a ImageButton
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
            val name = etProductName.text.toString().trim()
            val quantityStr = etQuantity.text.toString().trim()
            val priceStr = etPrice.text.toString().trim()
            
            // Validate inputs
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
            // Show confirmation dialog
            AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Está seguro que desea eliminar este producto?")
                .setPositiveButton("Sí") { _, _ ->
                    deleteProduct(product.id, product.imageUrl, dialog)
                }
                .setNegativeButton("No", null)
                .show()
        }
        
        btnClose.setOnClickListener{
            currentImageButton = null  // Limpiar la referencia al botón
            dialog.dismiss()
        }
        
        dialog.show()
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
            }
        }?.addOnFailureListener { e ->
            Toast.makeText(this, "Error al subir imagen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateProduct(productId: String, name: String, quantity: Int, price: Double, imageUrl: String, dialog: AlertDialog) {
        val productUpdates = mapOf(
            "name" to name,
            "quantity" to quantity,
            "price" to price,
            "imageUrl" to imageUrl
        )
        
        database.child("products").child(productId).updateChildren(productUpdates)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto actualizado exitosamente", Toast.LENGTH_SHORT).show()
                selectedImageUri = null
                currentImageButton = null  // Limpiar la referencia al botón
                dialog.dismiss()
                // Products will be refreshed automatically by the ValueEventListener
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar producto: ${e.message}", Toast.LENGTH_SHORT).show()
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
                            dialog.dismiss()
                        }.addOnFailureListener { e ->
                            // Even if image deletion fails, we consider it a success since the product data is gone
                            Toast.makeText(this, "Producto eliminado. Error al eliminar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        // If we can't get a reference to the image, just consider it a success
                        Toast.makeText(this, "Producto eliminado exitosamente", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                } else {
                    Toast.makeText(this, "Producto eliminado exitosamente", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar producto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}