package com.example.projectofinal.model

import java.util.Date

/**
 * Clase modelo que representa un pedido
 */
data class Order(
    val id: String = "", // ID único del pedido
    val customerName: String = "", // Nombre del cliente
    val phone: String = "", // Teléfono del cliente
    val address: String = "", // Dirección de entrega
    val description: String = "", // Descripción del pedido
    val totalAmount: Double = 0.0, // Monto total del pedido
    val orderDate: Long = Date().time, // Fecha del pedido (en milisegundos)
    val status: String = "Pendiente" // Estado del pedido (Pendiente, En Proceso, Completado, etc.)
) {
    // Constructor sin argumentos requerido para Firebase
    constructor() : this("", "", "", "", "", 0.0, Date().time, "Pendiente")
}

/**
 * Clase para representar un producto dentro de un pedido
 */
data class OrderProduct(
    val productId: String = "", // ID del producto
    val productName: String = "", // Nombre del producto
    val quantity: Int = 0, // Cantidad ordenada
    val pricePerUnit: Double = 0.0 // Precio por unidad al momento de la orden
) {
    // Constructor sin argumentos requerido para Firebase
    constructor() : this("", "", 0, 0.0)
    
    // Calcular subtotal del producto
    fun getSubtotal(): Double = quantity * pricePerUnit
}
