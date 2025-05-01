package com.example.projectofinal.model

/**
 * Clase modelo que representa un producto en el inventario
 */
data class Product(
    val id: String = "", // ID único del producto
    val name: String = "", // Nombre del producto
    val quantity: Int = 0, // Cantidad de unidades
    val price: Double = 0.0, // Precio por unidad
    val imageUrl: String = "" // URL de la imagen almacenada en Firebase Storage
) {
    // Constructor sin argumentos requerido para Firebase
    constructor() : this("", "", 0, 0.0, "")
}
