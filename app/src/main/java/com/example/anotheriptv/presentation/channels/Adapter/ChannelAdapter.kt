package com.example.anotheriptv.presentation.channels.Adapter


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anotheriptv.databinding.ItemChannelBinding
import com.example.anotheriptv.domain.model.Channel

class ChannelAdapter(
    private val onChannelClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelAdapter.ChannelViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChannelViewHolder(
        private val binding: ItemChannelBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel) {
            binding.tvChannelName.text = channel.name
            binding.tvCategory.text = channel.category

            if (channel.logo.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(channel.logo)
                    .placeholder(com.example.anotheriptv.R.drawable.ic_tv_placeholder)
                    .error(com.example.anotheriptv.R.drawable.ic_tv_placeholder)
                    .into(binding.ivLogo)
            }

            binding.root.setOnClickListener { onChannelClick(channel) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Channel, newItem: Channel) =
            oldItem == newItem
    }
}