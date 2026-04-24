package com.example.anotheriptv.presentation.xstream.movie.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.example.anotheriptv.domain.model.Channel
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anotheriptv.databinding.ItemMovieAllXstreamBinding
import com.example.anotheriptv.R

class ItemMovieXstreamAllAdapter(
    private val onChannelClick: (Channel) -> Unit
) : ListAdapter<Channel, ItemMovieXstreamAllAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovieAllXstreamBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMovieAllXstreamBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel) {
            binding.tvChannelName.text = channel.name

            // ← thêm phần rating
            if (channel.rating != null && channel.rating > 0) {
                binding.layoutRating.visibility = View.VISIBLE
                binding.tvRating.text = String.format("%.1f", channel.rating)
            } else {
                binding.layoutRating.visibility = View.GONE
            }

            val logoUrl = channel.logo
            val isValidUrl = logoUrl.isNotEmpty() &&
                    (logoUrl.startsWith("http") || logoUrl.startsWith("https"))

            if (isValidUrl) {
                binding.ivLogo.visibility = View.VISIBLE
                binding.tvLogoPlaceholder.visibility = View.GONE
                Glide.with(binding.root.context)
                    .load(logoUrl)
                    .placeholder(R.drawable.ic_tv_placeholder)
                    .error(R.drawable.ic_tv_placeholder)
                    .into(binding.ivLogo)
            } else {
                binding.ivLogo.visibility = View.GONE
                binding.tvLogoPlaceholder.visibility = View.VISIBLE
                binding.tvLogoPlaceholder.text = channel.name
                Glide.with(binding.root.context).clear(binding.ivLogo)
            }

            binding.root.setOnClickListener { onChannelClick(channel) }
        }

    }

    class DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Channel, newItem: Channel) = oldItem == newItem
    }

}