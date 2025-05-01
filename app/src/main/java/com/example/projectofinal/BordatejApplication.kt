package com.example.projectofinal

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class BordatejApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        
        // Configurar el idioma para Firebase Auth
        try {
            // Establecer el idioma local
            Locale.setDefault(Locale("es", "ES"))
            
            // Configurar Firebase Auth
            val auth = FirebaseAuth.getInstance()
            auth.useAppLanguage() // Usa el idioma de la aplicación
            auth.setLanguageCode("es") // También establecer explícitamente a español
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}