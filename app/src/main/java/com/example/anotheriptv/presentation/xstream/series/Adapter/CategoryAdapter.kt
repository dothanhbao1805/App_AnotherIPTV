package com.example.anotheriptv.presentation.xstream.series.Adapter

import com.example.anotheriptv.presentation.xstream.series.Adapter.ItemSeriesXstreamAdapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.domain.model.CategoryWithChannels
import com.example.anotheriptv.domain.model.Channel
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.anotheriptv.databinding.ItemCategoryWithSeriesBinding


class CategoryAdapter(
    private val onChannelClick: (Channel) -> Unit,
    private val onViewAllClick: (CategoryWithChannels) -> Unit
) : ListAdapter<CategoryWithChannels, CategoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryWithSeriesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemCategoryWithSeriesBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val channelAdapter = ItemSeriesXstreamAdapter(onChannelClick)

        init {
            binding.recyclerSeries.apply {
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