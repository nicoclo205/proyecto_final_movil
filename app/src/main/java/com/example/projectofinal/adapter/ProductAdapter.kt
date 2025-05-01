package com.example.projectofinal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectofinal.R
import com.example.projectofinal.model.Product
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(private var products: List<Product>) : 
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    
    interface OnProductClickListener {
        fun onProductClick(product: Product, position: Int)
    }
    
    private var listener: OnProductClickListener? = null
    
    fun setOnProductClickListener(listener: OnProductClickListener) {
        this.listener = listener
    }
    
    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageProduct: ImageView = itemView.findViewById(R.id.imageProduct)
        val textProductName: TextView = itemView.findViewById(R.id.textProductName)
        val textProductQuantity: TextView = itemView.findViewById(R.id.textProductQuantity)
        val textProductPrice: TextView = itemView.findViewById(R.id.textProductPrice)
        
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onProductClick(products[position], position)
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        
        // Set product name
        holder.textProductName.text = product.name
        
        // Set quantity
        holder.textProductQuantity.text = "Cantidad: ${product.quantity}"
        
        // Format price with currency
        val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        holder.textProductPrice.text = "Precio: ${format.format(product.price)}"
        
        // Load image if URL is not empty
        if (product.imageUrl.isNotEmpty()) {
            // Asegurarse de que la imagen sea visible
            holder.imageProduct.visibility = View.VISIBLE
            try {
                Glide.with(holder.itemView.context)
                    .load(product.imageUrl)
                    .into(holder.imageProduct)
            } catch (e: Exception) {
                // Si hay un error al cargar la imagen, simplemente no hacemos nada
            }
        } else {
            // Si no hay URL de imagen, simplemente ocultamos la ImageView
            holder.imageProduct.visibility = View.GONE
        }
    }
    
    override fun getItemCount(): Int = products.size
    
    // Update product list
    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}