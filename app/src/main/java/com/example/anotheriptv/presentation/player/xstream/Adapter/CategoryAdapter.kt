package com.example.anotheriptv.presentation.player.xstream.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val items: List<CategoryItem>,
    private val currentCategory: String,
    private val onClick: (CategoryItem) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    data class CategoryItem(val name: String, val channelCount: Int)

    inner class VH(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvCategoryName.text  = item.name
        holder.binding.tvChannelCount.text  = "${item.channelCount} channels"

        // Highlight category hiện tại
        if (item.name == currentCategory) {
            holder.binding.tvCategoryName.setTextColor(0xFF2196F3.toInt())
            holder.binding.ivCheck.visibility = View.VISIBLE
            holder.binding.root.setBackgroundResource(R.drawable.bg_info_card_selected)
        } else {
            holder.binding.tvCategoryName.setTextColor(0xFFFFFFFF.toInt())
            holder.binding.ivCheck.visibility = View.GONE
            holder.binding.root.setBackgroundResource(R.drawable.bg_info_card)
        }

        holder.binding.root.setOnClickListener { onClick(item) }
    }
}