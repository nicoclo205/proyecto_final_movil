package com.example.projectofinal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectofinal.R
import com.example.projectofinal.model.Order
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(private var orders: List<Order>) :
    RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    interface OnOrderClickListener {
        fun onOrderClick(order: Order, position: Int)
    }

    private var listener: OnOrderClickListener? = null

    fun setOnOrderClickListener(listener: OnOrderClickListener) {
        this.listener = listener
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textCustomerName: TextView = itemView.findViewById(R.id.textCustomerName)
        val textOrderDate: TextView = itemView.findViewById(R.id.textOrderDate)
        val textItemCount: TextView = itemView.findViewById(R.id.textItemCount)
        val textTotalAmount: TextView = itemView.findViewById(R.id.textTotalAmount)
        val textOrderStatus: TextView = itemView.findViewById(R.id.textOrderStatus)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onOrderClick(orders[position], position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        // Set customer name
        holder.textCustomerName.text = order.customerName

        // Format and set date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val orderDate = Date(order.orderDate)
        holder.textOrderDate.text = "Fecha: ${dateFormat.format(orderDate)}"

        // Set product count
        val description = if (order.description.length > 30) {
            "${order.description.substring(0, 30)}..."
        } else {
            order.description
        }
        holder.textItemCount.text = "Desc: $description"

        // Format and set total amount
        val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        holder.textTotalAmount.text = "Total: ${format.format(order.totalAmount)}"

        // Set order status
        holder.textOrderStatus.text = order.status
        
        // You could set different colors for different statuses if needed
        when (order.status) {
            "Pendiente" -> {
                holder.textOrderStatus.setBackgroundResource(R.drawable.search_button)
            }
            "En Proceso" -> {
                holder.textOrderStatus.setBackgroundResource(R.drawable.add_button)
            }
            "Completado" -> {
                holder.textOrderStatus.setBackgroundResource(R.drawable.rounded_button_green)
            }
            else -> {
                holder.textOrderStatus.setBackgroundResource(R.drawable.search_button)
            }
        }
    }

    override fun getItemCount(): Int = orders.size

    // Update order list
    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}