package com.example.anotheriptv.presentation.playlist.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.anotheriptv.databinding.ItemPlaylistBinding
import com.example.anotheriptv.domain.model.Playlist

class PlaylistAdapter(
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onMoreClick: (Playlist, View) -> Unit
) : ListAdapter<Playlist, PlaylistAdapter.PlaylistViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PlaylistViewHolder(
        val binding: ItemPlaylistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(playlist: Playlist) {
            // 1. Gán dữ liệu hiển thị
            binding.tvPlaylistName.text = playlist.name

            // Bạn có thể mở comment các dòng dưới nếu model Playlist của bạn có các trường này:
            // binding.tvUrl.text = playlist.url
            // binding.tvBadgeType.text = playlist.type
            // binding.tvDate.text = playlist.date

            // 2. Sự kiện click vào toàn bộ Card (vào xem chi tiết)
            binding.root.setOnClickListener {
                onPlaylistClick(playlist)
            }

            // 3. Sự kiện click riêng cho nút More (hiện nút Delete)
            binding.btnMore.setOnClickListener { view ->
                // Trả về đối tượng playlist và chính cái view btnMore để làm mỏ neo cho PopupWindow
                onMoreClick(playlist, view)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist) =
            oldItem == newItem
    }
}