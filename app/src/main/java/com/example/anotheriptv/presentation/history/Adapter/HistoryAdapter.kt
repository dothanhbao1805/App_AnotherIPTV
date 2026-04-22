package com.example.anotheriptv.presentation.history.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anotheriptv.R
import com.example.anotheriptv.data.local.entity.HistoryWithUrl
import com.example.anotheriptv.databinding.ItemHistoryBinding

class HistoryAdapter(
    private val onItemClick: (HistoryWithUrl) -> Unit,
    private val onRemoveClick: (HistoryWithUrl) -> Unit
) : ListAdapter<HistoryWithUrl, HistoryAdapter.ViewHolder>(DiffCallback()) { // Thêm () để khởi tạo class DiffCallback

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryWithUrl) {
            // 1. Gắn tên kênh
            binding.tvChannelName.text = item.channelName

            // 2. Tải logo
            if (item.channelLogo.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.channelLogo)
                    .placeholder(R.drawable.ic_tv_placeholder)
                    .error(R.drawable.ic_tv_placeholder)
                    .into(binding.ivLogo)
            } else {
                binding.ivLogo.setImageResource(R.drawable.ic_tv_placeholder)
            }

            // 3. Sự kiện click xem (Dùng cardHistory như bạn muốn)
            binding.cardHistory.setOnClickListener {
                onItemClick(item)
            }

            // 4. Sự kiện click xóa
            binding.ivRemove.setOnClickListener {
                onRemoveClick(item)
            }
        }
    }

    // Class so sánh dữ liệu
    class DiffCallback : DiffUtil.ItemCallback<HistoryWithUrl>() {
        override fun areItemsTheSame(oldItem: HistoryWithUrl, newItem: HistoryWithUrl): Boolean {
            return oldItem.historyId == newItem.historyId
        }

        override fun areContentsTheSame(oldItem: HistoryWithUrl, newItem: HistoryWithUrl): Boolean {
            return oldItem == newItem
        }
    }
}