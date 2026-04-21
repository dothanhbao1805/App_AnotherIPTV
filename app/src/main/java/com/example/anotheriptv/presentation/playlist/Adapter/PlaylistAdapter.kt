package com.example.anotheriptv.presentation.playlist.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.anotheriptv.databinding.ItemPlaylistBinding
import com.example.anotheriptv.databinding.ItemXstreamBinding
import com.example.anotheriptv.domain.model.Playlist

class PlaylistAdapter(
    private val onPlaylistClick: (Playlist) -> Unit,
    private val onMoreClick: (Playlist, View) -> Unit
) : ListAdapter<Playlist, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        const val VIEW_TYPE_M3U     = 0
        const val VIEW_TYPE_XSTREAM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).type == "M3U") VIEW_TYPE_M3U else VIEW_TYPE_XSTREAM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_M3U -> {
                val binding = ItemPlaylistBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                M3UViewHolder(binding)
            }
            else -> {
                val binding = ItemXstreamBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                XstreamViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val playlist = getItem(position)
        when (holder) {
            is M3UViewHolder     -> holder.bind(playlist)
            is XstreamViewHolder -> holder.bind(playlist)
        }
    }

    // ── ViewHolder M3U ──────────────────────────────────────────────────────────
    inner class M3UViewHolder(
        private val binding: ItemPlaylistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(playlist: Playlist) {
            binding.tvPlaylistName.text = playlist.name
            binding.root.setOnClickListener { onPlaylistClick(playlist) }
            binding.btnMore.setOnClickListener { onMoreClick(playlist, it) }
            if(playlist.sourceType == "URL")
            {
                binding.tvUrl.text = playlist.m3uUrl
            }
            else{
                binding.tvUrl.text = playlist.filePath
            }
        }

    }

    // ── ViewHolder Xstream ──────────────────────────────────────────────────────
    inner class XstreamViewHolder(
        private val binding: ItemXstreamBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(playlist: Playlist) {
            binding.tvPlaylistName.text = playlist.name
            binding.tvUrl.text = playlist.url
            binding.root.setOnClickListener { onPlaylistClick(playlist) }
            binding.btnMore.setOnClickListener { onMoreClick(playlist, it) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Playlist>() {
        override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist) =
            oldItem == newItem
    }
}