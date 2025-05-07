package com.example.projectofinal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectofinal.R
import com.example.projectofinal.model.OrderProduct
import java.text.NumberFormat
import java.util.Locale

class OrderProductAdapter(private var orderProducts: MutableList<OrderProduct>) :
    RecyclerView.Adapter<OrderProductAdapter.OrderProductViewHolder>() {

    private var readOnlyMode = false
    
    fun setReadOnlyMode(readOnly: Boolean) {
        this.readOnlyMode = readOnly
        notifyDataSetChanged()
    }

    interface OnOrderProductClickListener {
        fun onRemoveProductClick(orderProduct: OrderProduct, position: Int)
    }

    private var listener: OnOrderProductClickListener? = null

    fun setOnOrderProductClickListener(listener: OnOrderProductClickListener) {
        this.listener = listener
    }

    inner class OrderProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotal)
        val btnRemoveProduct: ImageButton = itemView.findViewById(R.id.btnRemoveProduct)

        init {
            btnRemoveProduct.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onRemoveProductClick(orderProducts[position], position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_product, parent, false)
        return OrderProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderProductViewHolder, position: Int) {
        val orderProduct = orderProducts[position]
        val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

        // Set product name
        holder.tvProductName.text = orderProduct.productName

        // Set quantity
        holder.tvQuantity.text = "Cant: ${orderProduct.quantity}"

        // Set price per unit
        holder.tvPrice.text = "Precio: ${format.format(orderProduct.pricePerUnit)}"

        // Set subtotal
        val subtotal = orderProduct.getSubtotal()
        holder.tvSubtotal.text = "Subtotal: ${format.format(subtotal)}"
        
        // Hide remove button in read-only mode
        holder.btnRemoveProduct.visibility = if (readOnlyMode) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = orderProducts.size

    // Add a new product to the order
    fun addProduct(orderProduct: OrderProduct) {
        orderProducts.add(orderProduct)
        notifyItemInserted(orderProducts.size - 1)
    }

    // Remove a product from the order
    fun removeProduct(position: Int) {
        if (position >= 0 && position < orderProducts.size) {
            orderProducts.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // Get all order products
    fun getOrderProducts(): List<OrderProduct> {
        return orderProducts
    }

    // Calculate total amount
    fun calculateTotal(): Double {
        return orderProducts.sumOf { it.getSubtotal() }
    }

    // Clear all products
    fun clearProducts() {
        val size = orderProducts.size
        orderProducts.clear()
        notifyItemRangeRemoved(0, size)
    }
}