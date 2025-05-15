package com.example.projectofinal

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ProfileActivity : AppCompatActivity() {

    // Vistas de información de usuario
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var imgProfile: ImageView
    private lateinit var btnChangePhoto: Button
    private lateinit var btnEditProfile: Button
    private lateinit var btnBack: ImageButton
    private lateinit var buttonLogout: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingBackground: View

    // Variables para Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private var currentUser: FirebaseUser? = null
    
    // Variables para almacenar datos de usuario
    private var userName: String = ""
    private var userPhone: String = ""
    private var userEmail: String = ""
    private var userPhotoUrl: String = ""
    
    private var selectedImageUri: Uri? = null

    // Activity result launcher para selección de imagen
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                
                // Hacer visible la imagen inmediatamente y ocultar el botón
                imgProfile.visibility = View.VISIBLE
                btnChangePhoto.visibility = View.GONE
                
                // Establecer la imagen seleccionada en el ImageView
                imgProfile.setImageURI(uri)
                
                // Subir la imagen inmediatamente
                uploadProfileImage()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference
        currentUser = auth.currentUser

        // Si no hay usuario logueado, redirigir al login
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Inicializar vistas
        initViews()

        // Cargar datos del usuario
        loadUserData()

        // Configurar listeners de botones
        setupButtonListeners()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Actualizar visibilidad y contenido
        updatePhotoVisibility()
    }
    
    /**
     * Actualiza la visibilidad del botón e imagen según si existe foto de perfil
     */
    private fun updatePhotoVisibility() {
        if (userPhotoUrl.isEmpty()) {
            // Si no hay foto, mostrar botón y ocultar imagen
            imgProfile.visibility = View.GONE
            btnChangePhoto.visibility = View.VISIBLE
            imgProfile.setImageDrawable(null)
        } else {
            // Si hay foto, mostrar imagen y ocultar botón
            imgProfile.visibility = View.VISIBLE
            btnChangePhoto.visibility = View.GONE
            
            // Cargar la imagen con Glide
            Glide.with(this)
                .load(userPhotoUrl)
                .circleCrop()
                .into(imgProfile)
        }
    }

    private fun initViews() {
        // TextView para mostrar la información
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        
        // Imagen de perfil y botones
        imgProfile = findViewById(R.id.imgProfile)
        btnChangePhoto = findViewById(R.id.btnchangePhoto)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnBack = findViewById(R.id.btnBack)
        buttonLogout = findViewById(R.id.buttonLogout)
        
        // Vistas para indicador de carga
        progressBar = findViewById(R.id.progressBar)
        loadingBackground = findViewById(R.id.loadingBackground)
        
        // Establecer email del usuario actual
        userEmail = currentUser?.email ?: ""
        tvEmail.text = "Email: $userEmail"
    }

    private fun loadUserData() {
        showLoading(true)

        val userId = currentUser?.uid ?: return

        database.getReference("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        userName = snapshot.child("name").getValue(String::class.java) ?: ""
                        userPhone = snapshot.child("phone").getValue(String::class.java) ?: ""
                        userPhotoUrl = snapshot.child("photoUrl").getValue(String::class.java) ?: ""

                        // Actualizar la interfaz con los datos
                        updateUI()
                    }
                    showLoading(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Error al cargar datos: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showLoading(false)
                }
            })
    }
    
    private fun updateUI() {
        // Actualizar los TextView con la información
        tvName.text = "Nombre: $userName"
        tvPhone.text = "Teléfono: $userPhone"
        tvEmail.text = "Email: $userEmail"
        
        // Actualizar visibilidad de foto de perfil
        updatePhotoVisibility()
    }

    private fun setupButtonListeners() {
        // Botón de volver atrás
        btnBack.setOnClickListener {
            onBackPressed()
        }
        
        // Cambiar foto de perfil desde el botón
        btnChangePhoto.setOnClickListener {
            openImagePicker()
        }
        
        // Cambiar foto de perfil desde la imagen existente
        imgProfile.setOnClickListener {
            openImagePicker()
        }
        
        // Botón de editar perfil
        btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        // Cerrar sesión
        buttonLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }
    
    /**
     * Abre el selector de imágenes del dispositivo
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }
    
    /**
     * Muestra un diálogo para editar la información del perfil
     */
    private fun showEditProfileDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_edit_profile, null)
        builder.setView(dialogView)
        
        val dialog = builder.create()
        
        // Obtener las vistas del diálogo
        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveProfile)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        val progressBarDialog = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val loadingBackgroundDialog = dialogView.findViewById<View>(R.id.loadingBackground)
        
        // Establecer los valores actuales
        etName.setText(userName)
        etPhone.setText(userPhone)
        
        // Configurar el botón de guardar
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            
            if (name.isEmpty()) {
                etName.error = "Por favor ingrese su nombre"
                return@setOnClickListener
            }
            
            // Mostrar carga en el diálogo
            progressBarDialog.visibility = View.VISIBLE
            loadingBackgroundDialog.visibility = View.VISIBLE
            
            // Actualizar datos en Firebase
            updateUserData(name, phone, dialog, progressBarDialog, loadingBackgroundDialog)
        }
        
        // Configurar el botón de cerrar
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        // Mostrar diálogo
        dialog.show()
        
        // Ajustar el ancho del diálogo
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    
    /**
     * Actualiza los datos del usuario en Firebase
     */
    private fun updateUserData(name: String, phone: String, dialog: AlertDialog, progressBarDialog: ProgressBar, loadingBackgroundDialog: View) {
        val userId = currentUser?.uid ?: return
        
        val userUpdates = mapOf(
            "name" to name,
            "phone" to phone
        )
        
        database.getReference("users").child(userId).updateChildren(userUpdates)
            .addOnSuccessListener {
                // Actualizar variables locales
                userName = name
                userPhone = phone
                
                // Actualizar UI
                updateUI()
                
                Toast.makeText(
                    this,
                    "Perfil actualizado exitosamente",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Ocultar carga y cerrar diálogo
                progressBarDialog.visibility = View.GONE
                loadingBackgroundDialog.visibility = View.GONE
                dialog.dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al actualizar perfil: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Ocultar carga
                progressBarDialog.visibility = View.GONE
                loadingBackgroundDialog.visibility = View.GONE
            }
    }
    
    /**
     * Sube la imagen de perfil a Firebase Storage
     */
    private fun uploadProfileImage() {
        if (selectedImageUri == null) return
        
        showLoading(true)
        
        val userId = currentUser?.uid ?: return
        val imageRef = storageRef.child("profile_images/$userId.jpg")
        
        selectedImageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    // Obtener URL de descarga
                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Actualizar URL en Firebase Database
                        updatePhotoUrl(downloadUrl.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Error al subir imagen: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // En caso de error, revertir la visibilidad si no había foto previa
                    if (userPhotoUrl.isEmpty()) {
                        imgProfile.visibility = View.GONE
                        btnChangePhoto.visibility = View.VISIBLE
                    }
                    
                    showLoading(false)
                }
        }
    }
    
    /**
     * Actualiza la URL de la foto en Firebase Database
     */
    private fun updatePhotoUrl(photoUrl: String) {
        val userId = currentUser?.uid ?: return
        
        val updates = mapOf(
            "photoUrl" to photoUrl
        )
        
        database.getReference("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                userPhotoUrl = photoUrl
                
                // Asegurar que la visibilidad es correcta
                runOnUiThread {
                    updatePhotoVisibility()
                }
                
                Toast.makeText(
                    this,
                    "Foto de perfil actualizada",
                    Toast.LENGTH_SHORT
                ).show()
                
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al actualizar foto: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                
                showLoading(false)
            }
    }

    /**
     * Muestra un diálogo de confirmación para cerrar sesión
     */
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Está seguro que desea cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Cierra la sesión actual y redirige al usuario a la pantalla de login
     */
    private fun logout() {
        // Cerrar sesión en Firebase
        auth.signOut()
        
        // Mostrar mensaje
        Toast.makeText(this, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show()
        
        // Redirigir a la pantalla de inicio de sesión y limpiar la pila de actividades
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Muestra u oculta el indicador de carga
     */
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            progressBar.visibility = View.VISIBLE
            loadingBackground.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
            loadingBackground.visibility = View.GONE
        }
    }
    
    /**
     * Maneja el botón de retroceso
     */
    override fun onBackPressed() {
        // Navegar a InfoActivity
        startActivity(Intent(this, InfoActivity::class.java))
        finish()
    }
}