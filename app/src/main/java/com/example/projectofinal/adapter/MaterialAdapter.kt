package com.example.projectofinal.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projectofinal.R
import com.example.projectofinal.model.Material

class MaterialAdapter(private val materials: MutableList<Material>) : 
    RecyclerView.Adapter<MaterialAdapter.MaterialViewHolder>() {

    class MaterialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val materialName: TextView = itemView.findViewById(R.id.materialName)
        val materialCost: TextView = itemView.findViewById(R.id.materialCost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaterialViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_material, parent, false)
        return MaterialViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MaterialViewHolder, position: Int) {
        val currentItem = materials[position]
        holder.materialName.text = currentItem.name
        holder.materialCost.text = "$${String.format("%.2f", currentItem.cost)}"
    }

    override fun getItemCount() = materials.size

    fun addMaterial(material: Material) {
        materials.add(material)
        notifyItemInserted(materials.size - 1)
    }

    fun getTotalCost(): Double {
        return materials.sumOf { it.cost }
    }
}