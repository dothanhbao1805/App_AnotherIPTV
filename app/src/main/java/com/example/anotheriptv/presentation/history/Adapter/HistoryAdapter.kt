package com.example.anotheriptv.presentation.history.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.ItemHistoryBinding
import com.example.anotheriptv.domain.model.Channel

class HistoryAdapter(
    private val onItemClick: (Channel) -> Unit,
    private val onRemoveClick: (Channel) -> Unit
) : ListAdapter<Channel, HistoryAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel) {
            binding.tvChannelName.text = channel.name

            Glide.with(binding.ivLogo)
                .load(channel.logo)
                .placeholder(R.drawable.ic_tv_placeholder)
                .error(R.drawable.ic_tv_placeholder)
                .into(binding.ivLogo)

            binding.cardHistory.setOnClickListener { onItemClick(channel) }
            binding.ivRemove.setOnClickListener { onRemoveClick(channel) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Channel, newItem: Channel) =
            oldItem == newItem
    }
}