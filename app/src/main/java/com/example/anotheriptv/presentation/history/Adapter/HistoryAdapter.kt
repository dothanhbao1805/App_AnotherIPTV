package com.example.anotheriptv.presentation.history.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anotheriptv.R
import com.example.anotheriptv.data.local.entity.HistoryWithUrl
import com.example.anotheriptv.databinding.ItemHistoryBinding
import com.example.anotheriptv.databinding.ItemHistoryFavoriteBinding

class HistoryAdapter(
    private val onItemClick: (HistoryWithUrl) -> Unit,
    private val onRemoveClick: (HistoryWithUrl) -> Unit,
    private val onFavoriteClick: ((HistoryWithUrl) -> Unit)? = null // Thêm callback xử lý Favorite
) : ListAdapter<HistoryWithUrl, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val TYPE_NORMAL = 0
        private const val TYPE_FAVORITE = 1
    }

    // Xác định xem item này thuộc loại nào dựa trên giá trị isFavorite
    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isFavorite && onFavoriteClick != null) TYPE_FAVORITE else TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_FAVORITE) {
            val binding = ItemHistoryFavoriteBinding.inflate(inflater, parent, false)
            FavoriteViewHolder(binding)
        } else {
            val binding = ItemHistoryBinding.inflate(inflater, parent, false)
            NormalViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is FavoriteViewHolder) holder.bind(item)
        else if (holder is NormalViewHolder) holder.bind(item)
    }

    // --- ViewHolder cho danh sách thường ---
    inner class NormalViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryWithUrl) {
            binding.tvChannelName.text = item.channelName
            loadImage(item.channelLogo, binding.ivLogo)
            binding.cardHistory.setOnClickListener { onItemClick(item) }
            binding.ivRemove.setOnClickListener { onRemoveClick(item) }
        }
    }

    // --- ViewHolder cho danh sách Favorite ---
    inner class FavoriteViewHolder(private val binding: ItemHistoryFavoriteBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryWithUrl) {
            binding.tvChannelName.text = item.channelName
            loadImage(item.channelLogo, binding.ivLogo)

            // Click vào card để xem
            binding.cardHistory.setOnClickListener { onItemClick(item) }

            // Click vào ivRemove (hình tim đỏ) để bỏ yêu thích
            binding.ivRemove.setOnClickListener {
                onFavoriteClick?.invoke(item)
            }
        }
    }

    private fun loadImage(url: String, imageView: android.widget.ImageView) {
        Glide.with(imageView.context)
            .load(url)
            .placeholder(R.drawable.ic_tv_placeholder)
            .error(R.drawable.ic_tv_placeholder)
            .into(imageView)
    }

    class DiffCallback : DiffUtil.ItemCallback<HistoryWithUrl>() {
        override fun areItemsTheSame(old: HistoryWithUrl, new: HistoryWithUrl) = old.historyId == new.historyId
        override fun areContentsTheSame(old: HistoryWithUrl, new: HistoryWithUrl) = old == new
    }
}