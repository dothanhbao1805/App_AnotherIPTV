package com.example.anotheriptv.presentation.xstream.series.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.anotheriptv.databinding.ItemSeasonBinding
import com.example.anotheriptv.domain.model.Season

class SeasonAdapter(
    private val onSeasonClick: (Season) -> Unit
) : ListAdapter<Season, SeasonAdapter.ViewHolder>(DiffCallback()) {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSeasonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    inner class ViewHolder(
        private val binding: ItemSeasonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(season: Season, isSelected: Boolean) {
            binding.tvSeasonName.text = season.name

            binding.tvEpisodeCount.text = "${season.episodeCount} Episodes"

            binding.tvSeasonDate.text = season.releaseDate ?: "N/A"

            // Highlight season đang chọn
            binding.root.isSelected = isSelected

            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val prev = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(prev)
                    notifyItemChanged(selectedPosition)
                    onSeasonClick(season)
                }
            }

        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Season>() {
        override fun areItemsTheSame(a: Season, b: Season) =
            a.seasonNumber == b.seasonNumber
        override fun areContentsTheSame(a: Season, b: Season) =
            a == b
    }
}