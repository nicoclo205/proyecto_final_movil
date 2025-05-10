package com.example.projectofinal

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonGoogleLogin: Button
    private lateinit var buttonBack: ImageButton
    private lateinit var textViewRegister: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingBackground: View
    
    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_SIGN_IN = 9001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        try {
            // Asegurarse de que Firebase está inicializado
            FirebaseApp.initializeApp(this)
            
            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance()
            auth.setLanguageCode("es") // Establecer el idioma a español
            
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

                showLoading(false)

                if (result.resultCode == RESULT_OK) {
                    val signInResult = result.data?.let {
                        Auth.GoogleSignInApi.getSignInResultFromIntent(
                            it
                        )
                    }
                    if (signInResult != null && signInResult.isSuccess) {
                        val account = signInResult.signInAccount
                        if (account != null) {

                            showLoading(true)

                            firebaseAuthWithGoogle(account.idToken!!)
                        } else {
                            Toast.makeText(this, "Error: Cuenta nula", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.w(TAG, "Google sign in failed: ${signInResult?.status}")
                        Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "Google sign in failed: Result code = ${result.resultCode}")
                    Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Initialize UI elements
            editTextEmail = findViewById(R.id.editTextEmail)
            editTextPassword = findViewById(R.id.editTextPassword)
            buttonLogin = findViewById(R.id.buttonLogin)
            buttonGoogleLogin = findViewById(R.id.buttonGoogleLogin)
            buttonBack = findViewById(R.id.buttonBack)
            textViewRegister = findViewById(R.id.textViewRegister)

            progressBar = findViewById(R.id.progressBar)
            loadingBackground = findViewById(R.id.loadingBackground)

            // Asegurar que la carga inicie de forma oculta
            progressBar.visibility = View.GONE
            loadingBackground.visibility = View.GONE


            buttonLogin.setOnClickListener {
                if (isNetworkAvailable()) {
                    loginUser()
                } else {
                    Toast.makeText(
                        this,
                        "No hay conexión a internet. Por favor, verifica tu conexión e intenta nuevamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            
            buttonGoogleLogin.setOnClickListener {
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
            
            textViewRegister.setOnClickListener {
                // navegar al registro
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
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

    //funcion para mostrar u ocultar el indicador de carga
    private fun showLoading(isLoading: Boolean){
        if(isLoading){
            progressBar.visibility = View.VISIBLE
            loadingBackground.visibility = View.VISIBLE

            buttonLogin.isEnabled = false
            buttonGoogleLogin.isEnabled = false
            buttonBack.isEnabled = false
            textViewRegister.isEnabled = false

        } else {

            progressBar.visibility = View.GONE
            loadingBackground.visibility = View.GONE

            buttonLogin.isEnabled = true
            buttonGoogleLogin.isEnabled = true
            buttonBack.isEnabled = true
            textViewRegister.isEnabled = true

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
    
    private fun loginUser() {
        try {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            
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
            
            if (password.isEmpty()) {
                editTextPassword.error = "Por favor ingresa tu contraseña"
                editTextPassword.requestFocus()
                return
            }

            showLoading(true)
            
            // Mostrar mensaje de carga
            Toast.makeText(
                this,
                "Iniciando sesión...",
                Toast.LENGTH_SHORT
            ).show()
            
            // Sign in with email and password
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, navigate to main activity
                        Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, InventoryActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        showLoading(false)
                        startActivity(intent)
                        finish()
                    } else {
                        // Manejar errores específicos
                        val exception = task.exception
                        Log.e(TAG, "Error de inicio de sesión: $exception", exception)
                        
                        val errorMessage = when (exception) {
                            is FirebaseAuthInvalidUserException -> "No existe cuenta con este correo electrónico. Por favor, regístrate."
                            is FirebaseAuthInvalidCredentialsException -> "Correo electrónico o contraseña incorrectos."
                            is FirebaseNetworkException -> "Error de conexión a Internet. Por favor, verifica tu conexión."
                            else -> "Error de inicio de sesión: ${exception?.message ?: "Error desconocido"}"
                        }
                        
                        Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_SHORT
                        ).show()

                        showLoading(false)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error en loginUser: ${e.message}", e)
            Toast.makeText(
                this,
                "Error de inicio de sesión: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            showLoading(false)
        }
    }
    
    private fun signInWithGoogle() {
        try {
            showLoading(true)
            // Mostrar mensaje de carga
            Toast.makeText(this, "Iniciando sesión con Google...", Toast.LENGTH_SHORT).show()
            
            if (!googleApiClient.isConnected) {
                googleApiClient.connect()
            }
            
            val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
            googleSignInLauncher.launch(signInIntent)


        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar sesión con Google: ${e.message}", e)
            Toast.makeText(
                this,
                "Error al iniciar el proceso de autenticación con Google: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()

            showLoading(false)
        }
    }
    
    private fun firebaseAuthWithGoogle(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->

                    showLoading(false)

                    if (task.isSuccessful) {
                        // Sign in success
                        Log.d(TAG, "signInWithCredential:success")
                        Toast.makeText(this, "Inicio de sesión con Google exitoso", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, InventoryActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // falla
                        val exception = task.exception
                        Log.e(TAG, "signInWithCredential:failure", exception)
                        
                        val errorMessage = when (exception) {
                            is FirebaseAuthInvalidCredentialsException -> "Las credenciales de Google no son válidas."
                            is FirebaseNetworkException -> "Error de conexión a Internet. Por favor, verifica tu conexión."
                            else -> "Error de autenticación con Google: ${exception?.message ?: "Error desconocido"}"
                        }
                        
                        Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } catch (e: Exception) {


            showLoading(false)

            Log.e(TAG, "Error en firebaseAuthWithGoogle: ${e.message}", e)
            Toast.makeText(
                this,
                "Error al procesar la autenticación con Google: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}