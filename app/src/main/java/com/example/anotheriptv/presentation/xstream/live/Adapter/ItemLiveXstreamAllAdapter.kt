package com.example.anotheriptv.presentation.xstream.live.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.example.anotheriptv.domain.model.Channel
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anotheriptv.databinding.ItemLiveAllXstreamBinding
import com.example.anotheriptv.R

class ItemLiveXstreamAllAdapter(
    private val onChannelClick: (Channel) -> Unit
) : ListAdapter<Channel, ItemLiveXstreamAllAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLiveAllXstreamBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemLiveAllXstreamBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: Channel) {
            binding.tvChannelName.text = channel.name
            if (channel.logo.isNotEmpty()) {
                binding.ivLogo.visibility = android.view.View.VISIBLE
                binding.tvLogoPlaceholder.visibility = android.view.View.GONE

                Glide.with(binding.root.context)
                    .load(channel.logo)
                    .placeholder(R.drawable.ic_tv_placeholder)
                    .error(R.drawable.ic_tv_placeholder)
                    .into(binding.ivLogo)
            } else {
                binding.ivLogo.visibility = android.view.View.GONE
                binding.tvLogoPlaceholder.visibility = android.view.View.VISIBLE

                binding.ivLogo.setImageResource(R.drawable.ic_tv_placeholder)

                binding.tvLogoPlaceholder.text = channel.name
            }
            binding.root.setOnClickListener { onChannelClick(channel) }
        }

    }

    class DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(oldItem: Channel, newItem: Channel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Channel, newItem: Channel) = oldItem == newItem
    }


}