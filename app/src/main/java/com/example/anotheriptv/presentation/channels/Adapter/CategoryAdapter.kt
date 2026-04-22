package com.example.anotheriptv.presentation.channels.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.domain.model.CategoryWithChannels
import com.example.anotheriptv.domain.model.Channel
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.anotheriptv.databinding.ItemCategoryWithChannelsBinding
import com.example.anotheriptv.presentation.xstream.live.Adapter.ItemLiveXstreamAdapter


class CategoryAdapter(
    private val onChannelClick: (Channel) -> Unit,
    private val onViewAllClick: (CategoryWithChannels) -> Unit
) : ListAdapter<CategoryWithChannels, CategoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryWithChannelsBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCategoryWithChannelsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val channelAdapter = ItemLiveXstreamAdapter(onChannelClick)

        init {
            binding.recyclerLive.apply {
                layoutManager = LinearLayoutManager(
                    binding.root.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = channelAdapter
            }
        }

        fun bind(item: CategoryWithChannels) {
            binding.tvcategoryname.text = item.categoryName
            binding.tvViewAll.setOnClickListener { onViewAllClick(item) }
            channelAdapter.submitList(item.channels)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryWithChannels>() {
        override fun areItemsTheSame(a: CategoryWithChannels, b: CategoryWithChannels) =
            a.categoryId == b.categoryId
        override fun areContentsTheSame(a: CategoryWithChannels, b: CategoryWithChannels) =
            a == b
    }

}