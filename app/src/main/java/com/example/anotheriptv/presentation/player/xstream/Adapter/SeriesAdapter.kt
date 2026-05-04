package com.example.anotheriptv.presentation.player.xstream.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.ItemSeasonsPlayerBinding

class SeriesAdapter(
    private val items: List<SeriesItem>,
    private val currentSeries: String,
    private val onClick: (SeriesItem) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.VH>() {

    data class SeriesItem(val name: String, val episodeCount: Int)

    inner class VH(val binding: ItemSeasonsPlayerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemSeasonsPlayerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvTitleSeason.text   = item.name
        holder.binding.tvCountEpisodes.text = "${item.episodeCount} episodes"

        if (item.name == currentSeries) {
            holder.binding.tvTitleSeason.setTextColor(0xFF2196F3.toInt())
            holder.binding.ivCheck.visibility = View.VISIBLE
            holder.binding.root.setBackgroundResource(R.drawable.bg_info_card_selected)
        } else {
            holder.binding.tvTitleSeason.setTextColor(0xFFFFFFFF.toInt())
            holder.binding.ivCheck.visibility = View.GONE
            holder.binding.root.setBackgroundResource(R.drawable.bg_info_card)
        }

        holder.binding.root.setOnClickListener { onClick(item) }
    }
}