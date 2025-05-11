package com.example.projectofinal.model

data class Reminder(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: Long = 0, // Almacenamos la fecha como un timestamp en milisegundos
    val time: Long = 0, // Almacenamos la hora como milisegundos desde el inicio del día
    val completed: Boolean = false
) {
    // Constructor sin argumentos requerido por Firebase
    constructor() : this("", "", "", 0, 0, false)
}