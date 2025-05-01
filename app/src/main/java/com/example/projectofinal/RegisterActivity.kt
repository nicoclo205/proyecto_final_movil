package com.example.projectofinal

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var editTextEmail: EditText
    private lateinit var editTextName: EditText
    private lateinit var editTextLastName: EditText
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var buttonGoogleRegister: Button
    private lateinit var buttonBack: ImageButton
    private lateinit var textViewLogin: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    
    companion object {
        private const val TAG = "RegisterActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        try {
            // Asegurarse de que Firebase está inicializado
            FirebaseApp.initializeApp(this)
            
            // Initialize Firebase Auth and Database
            auth = FirebaseAuth.getInstance()
            auth.setLanguageCode("es") // Establecer el idioma a español
            database = FirebaseDatabase.getInstance()
            
            // Inicializar Google Sign-In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
                
            googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
            
            // Configurar el lanzador de actividad para Google Sign-In
            googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val signInResult = result.data?.let {
                        Auth.GoogleSignInApi.getSignInResultFromIntent(
                            it
                        )
                    }
                    if (signInResult != null && signInResult.isSuccess) {
                        val account = signInResult.signInAccount
                        if (account != null) {
                            firebaseAuthWithGoogle(account.idToken!!)
                        } else {
                            Toast.makeText(this, "Error: Cuenta nula", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.w(TAG, "Google sign in failed: ${signInResult?.status}")
                        Toast.makeText(this, "Error al registrarse con Google", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "Google sign in failed: Result code = ${result.resultCode}")
                    Toast.makeText(this, "Error al registrarse con Google", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Initialize UI elements
            editTextEmail = findViewById(R.id.editTextEmail)
            editTextName = findViewById(R.id.editTextName)
            editTextLastName = findViewById(R.id.editTextLastName)
            editTextUsername = findViewById(R.id.editTextUsername)
            editTextPassword = findViewById(R.id.editTextPassword)
            buttonRegister = findViewById(R.id.buttonRegister)
            buttonGoogleRegister = findViewById(R.id.buttonGoogleRegister)
            buttonBack = findViewById(R.id.buttonBack)
            textViewLogin = findViewById(R.id.textViewLogin)
            
            // Set click listeners
            buttonRegister.setOnClickListener {
                if (isNetworkAvailable()) {
                    registerUser()
                } else {
                    Toast.makeText(
                        this,
                        "No hay conexión a internet. Por favor, verifica tu conexión e intenta nuevamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            
            buttonGoogleRegister.setOnClickListener {
                if (isNetworkAvailable()) {
                    signInWithGoogle()
                } else {
                    Toast.makeText(
                        this,
                        "No hay conexión a internet. Por favor, verifica tu conexión e intenta nuevamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            
            buttonBack.setOnClickListener {
                onBackPressed()
            }
            
            textViewLogin.setOnClickListener {
                // Navigate to login screen
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            Toast.makeText(
                this,
                "Error al inicializar la aplicación: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onStart() {
        super.onStart()
        googleApiClient.connect()
    }
    
    override fun onStop() {
        super.onStop()
        if (googleApiClient.isConnected) {
            googleApiClient.disconnect()
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    
    private fun signInWithGoogle() {
        try {
            // Mostrar mensaje de carga
            Toast.makeText(this, "Iniciando registro con Google...", Toast.LENGTH_SHORT).show()
            
            if (!googleApiClient.isConnected) {
                googleApiClient.connect()
            }
            
            val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar registro con Google: ${e.message}", e)
            Toast.makeText(
                this,
                "Error al iniciar el proceso de registro con Google: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun firebaseAuthWithGoogle(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, save user data
                        val user = auth.currentUser
                        if (user != null) {
                            val userId = user.uid
                            val email = user.email ?: ""
                            val name = user.displayName?.split(" ")?.firstOrNull() ?: ""
                            val lastName = user.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""
                            val username = email.substringBefore("@")
                            
                            // Guardar datos del usuario en Firebase
                            val userRef = database.getReference("users").child(userId)
                            
                            val userData = HashMap<String, Any>()
                            userData["email"] = email
                            userData["name"] = name
                            userData["lastName"] = lastName
                            userData["username"] = username
                            
                            userRef.setValue(userData)
                                .addOnSuccessListener {
                                    Log.d(TAG, "signInWithCredential:success")
                                    Toast.makeText(this, "Registro con Google exitoso", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error al guardar datos del usuario: ${e.message}", e)
                                    Toast.makeText(
                                        this,
                                        "Error al guardar datos: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    } else {
                        // Sign in failed
                        val exception = task.exception
                        Log.e(TAG, "signInWithCredential:failure", exception)
                        
                        val errorMessage = when (exception) {
                            is FirebaseAuthInvalidCredentialsException -> "Las credenciales de Google no son válidas."
                            is FirebaseNetworkException -> "Error de conexión a Internet. Por favor, verifica tu conexión."
                            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este correo electrónico."
                            else -> "Error de autenticación con Google: ${exception?.message ?: "Error desconocido"}"
                        }
                        
                        Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error en firebaseAuthWithGoogle: ${e.message}", e)
            Toast.makeText(
                this,
                "Error al procesar la autenticación con Google: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun registerUser() {
        try {
            // Obtener y validar campos
            val email = editTextEmail.text.toString().trim()
            val name = editTextName.text.toString().trim()
            val lastName = editTextLastName.text.toString().trim()
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            
            // Validar inputs
            if (email.isEmpty()) {
                editTextEmail.error = "Por favor ingresa tu correo electrónico"
                editTextEmail.requestFocus()
                return
            }
            
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                editTextEmail.error = "Por favor ingresa un correo electrónico válido"
                editTextEmail.requestFocus()
                return
            }
            
            if (name.isEmpty()) {
                editTextName.error = "Por favor ingresa tu nombre"
                editTextName.requestFocus()
                return
            }
            
            if (lastName.isEmpty()) {
                editTextLastName.error = "Por favor ingresa tu apellido"
                editTextLastName.requestFocus()
                return
            }
            
            if (username.isEmpty()) {
                editTextUsername.error = "Por favor ingresa un nombre de usuario"
                editTextUsername.requestFocus()
                return
            }
            
            if (password.isEmpty()) {
                editTextPassword.error = "Por favor ingresa una contraseña"
                editTextPassword.requestFocus()
                return
            }
            
            if (password.length < 6) {
                editTextPassword.error = "La contraseña debe tener al menos 6 caracteres"
                editTextPassword.requestFocus()
                return
            }
            
            // Mostrar mensaje de carga
            Toast.makeText(
                this,
                "Procesando registro...",
                Toast.LENGTH_SHORT
            ).show()
            
            // Create user with email and password
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign up success, save user data to database
                        val userId = auth.currentUser?.uid
                        
                        if (userId != null) {
                            // Esperar a que el token de autenticación esté completo
                            auth.currentUser?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val userRef = database.getReference("users").child(userId)
                                    
                                    val userData = HashMap<String, Any>()
                                    userData["email"] = email
                                    userData["name"] = name
                                    userData["lastName"] = lastName
                                    userData["username"] = username
                                    
                                    userRef.setValue(userData)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this,
                                                "Registro exitoso",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            
                                            // Navigate to main activity
                                            val intent = Intent(this, LoginActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Error al guardar datos: ${e.message}", e)
                                            Toast.makeText(
                                                this,
                                                "Error al guardar datos: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                } else {
                                    Log.e(TAG, "Error al obtener token: ${tokenTask.exception}")
                                    Toast.makeText(
                                        this,
                                        "Error de autenticación: No se pudo completar el registro",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    } else {
                        // Manejar errores específicos
                        val exception = task.exception
                        Log.e(TAG, "Error de registro: $exception", exception)
                        
                        val errorMessage = when (exception) {
                            is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil. Por favor, usa una contraseña más fuerte."
                            is FirebaseAuthInvalidCredentialsException -> "El formato del correo electrónico no es válido."
                            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este correo electrónico."
                            is FirebaseNetworkException -> "Error de conexión a Internet. Por favor, verifica tu conexión."
                            else -> "Error de registro: ${exception?.message ?: "Error desconocido"}"
                        }
                        
                        Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error en registerUser: ${e.message}", e)
            Toast.makeText(
                this,
                "Error de registro: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}