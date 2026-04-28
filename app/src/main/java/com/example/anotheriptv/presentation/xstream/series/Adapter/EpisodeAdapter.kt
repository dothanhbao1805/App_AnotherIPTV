package com.example.anotheriptv.presentation.xstream.series.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.ItemEpisodeBinding
import com.example.anotheriptv.domain.model.Channel

class EpisodeAdapter(
    private val onEpisodeClick: (Channel) -> Unit
) : ListAdapter<Channel, EpisodeAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEpisodeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemEpisodeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(episode: Channel) {

            // Tên tập — bỏ prefix "Series - S01E01 - " nếu có
            val cleanTitle = episode.name
                .substringAfter(" - ")
                .substringAfter(" - ")
                .ifBlank { episode.name }
            binding.tvEpisodeTitle.text = cleanTitle

            // Thời lượng
            binding.tvEpisodeDuration.text = episode.episodeDuration?.let { "$it min" } ?: ""

            // Mô tả
            val desc = episode.description.orEmpty()
            if (desc.isNotBlank()) {
                binding.tvEpisodeDescription.visibility = View.VISIBLE
                binding.tvEpisodeDescription.text = desc
            } else {
                binding.tvEpisodeDescription.visibility = View.GONE
            }

            // Rating
            val rating = episode.rating
            if (rating != null && rating > 0f) {
                binding.tvEpisodeRating.visibility = View.VISIBLE
                binding.tvEpisodeRating.text = "★ ${String.format("%.1f", rating)}"
            } else {
                binding.tvEpisodeRating.visibility = View.GONE
            }

            // Thumbnail
            Glide.with(binding.root.context)
                .load(episode.logo.ifBlank { null })
                .placeholder(R.drawable.ic_tv_placeholder)
                .error(R.drawable.ic_tv_placeholder)
                .centerCrop()
                .into(binding.ivEpisodeThumbnail)

            binding.root.setOnClickListener { onEpisodeClick(episode) }
            binding.ivPlay.setOnClickListener { onEpisodeClick(episode) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(a: Channel, b: Channel) = a.id == b.id
        override fun areContentsTheSame(a: Channel, b: Channel) = a == b
    }
}