package com.example.anotheriptv.presentation.settings.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.anotheriptv.R
import com.example.anotheriptv.data.local.entity.CategoryEntity
import com.google.android.material.switchmaterial.SwitchMaterial

class HideCategoryAdapter(
    private val categories: List<CategoryEntity>,
    private val onToggle: (CategoryEntity, isHidden: Boolean) -> Unit
) : RecyclerView.Adapter<HideCategoryAdapter.ViewHolder>() {

    private val checkedStates = categories.map { !it.isHidden }.toMutableList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvCategoryName)
        val switchCategory: SwitchMaterial = view.findViewById(R.id.switchCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hide_category, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.tvName.text = category.name

        holder.switchCategory.setOnCheckedChangeListener(null)

        holder.switchCategory.isChecked = checkedStates[position]

        // 3. Gắn lại listener
        holder.switchCategory.setOnCheckedChangeListener { _, isChecked ->
            checkedStates[position] = isChecked
            onToggle(category, !isChecked) // isHidden = !isChecked
        }
    }

    override fun getItemCount() = categories.size

    fun setAllChecked(checked: Boolean) {
        checkedStates.fill(checked)
        notifyDataSetChanged()
    }
}