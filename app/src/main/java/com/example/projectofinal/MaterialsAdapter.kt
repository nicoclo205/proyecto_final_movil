package com.example.projectofinal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class MaterialsAdapter(
    private val materials: List<CalculatorActivity.Material>,
    private val onDeleteClicked: (Int) -> Unit
) : RecyclerView.Adapter<MaterialsAdapter.MaterialViewHolder>() {

    class MaterialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val materialName: TextView = itemView.findViewById(R.id.materialName)
        val materialCost: TextView = itemView.findViewById(R.id.materialCost)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteMaterialButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_material, parent, false)
        return MaterialViewHolder(view)
    }

    override fun getItemCount(): Int = materials.size

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        val material = materials[position]
        holder.materialName.text = material.name
        
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        holder.materialCost.text = currencyFormat.format(material.cost)
        
        holder.deleteButton.setOnClickListener {
            onDeleteClicked(position)
        }
    }
}