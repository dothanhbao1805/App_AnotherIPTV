package com.example.anotheriptv.presentation.history.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anotheriptv.databinding.ItemHistoryBinding
import com.example.anotheriptv.domain.model.WatchHistory

class HistoryAdapter(
    private val onItemClick: (WatchHistory) -> Unit,
    private val onRemoveClick: (WatchHistory) -> Unit
) : ListAdapter<WatchHistory, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(historyItem: WatchHistory) {
            // 1. Gắn tên kênh vào tvChannelName
            binding.tvChannelName.text = historyItem.channelName

            // 2. Tải logo vào ivLogo bằng Glide
            if (historyItem.channelLogo.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(historyItem.channelLogo)
                    .placeholder(com.example.anotheriptv.R.drawable.ic_tv_placeholder)
                    .error(com.example.anotheriptv.R.drawable.ic_tv_placeholder)
                    .into(binding.ivLogo)
            } else {
                binding.ivLogo.setImageResource(com.example.anotheriptv.R.drawable.ic_tv_placeholder)
            }

            // 3. Bắt sự kiện click vào Card để xem video (dùng cardHistory thay vì root để tránh click nhầm viền margin)
            binding.cardHistory.setOnClickListener {
                onItemClick(historyItem)
            }

            // 4. Bắt sự kiện click vào nút X để xóa lịch sử (đúng ID ivRemove của bạn)
            binding.ivRemove.setOnClickListener {
                onRemoveClick(historyItem)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<WatchHistory>() {
            override fun areItemsTheSame(oldItem: WatchHistory, newItem: WatchHistory): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: WatchHistory, newItem: WatchHistory): Boolean {
                return oldItem == newItem
            }
        }
    }
}