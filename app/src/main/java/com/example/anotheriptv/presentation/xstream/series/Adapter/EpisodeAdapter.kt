package com.example.anotheriptv.presentation.xstream.series.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.ItemEpisodeBinding
import com.example.anotheriptv.databinding.ItemEpisodeNewBinding
import com.example.anotheriptv.domain.model.Channel

class EpisodeAdapter(
    private val onEpisodeClick: (Channel) -> Unit
) : ListAdapter<Channel, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_NEW = 1
        private const val NEW_EPISODE_DAYS = 20L
    }

    private fun isNewEpisode(episode: Channel): Boolean {
        val dateStr = episode.releaseDate ?: return false
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return false
            val diffDays = (System.currentTimeMillis() - date.time) / (1000L * 60 * 60 * 24)
            diffDays <= NEW_EPISODE_DAYS
        } catch (e: Exception) { false }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isNewEpisode(getItem(position))) VIEW_TYPE_NEW else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_NEW) {
            val binding = ItemEpisodeNewBinding.inflate(inflater, parent, false)
            NewViewHolder(binding)
        } else {
            val binding = ItemEpisodeBinding.inflate(inflater, parent, false)
            NormalViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val episode = getItem(position)
        when (holder) {
            is NewViewHolder -> holder.bind(episode)
            is NormalViewHolder -> holder.bind(episode)
        }
    }

    // ── ViewHolder thường ────────────────────────────────────────────────────

    inner class NormalViewHolder(
        private val binding: ItemEpisodeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(episode: Channel) {
            bindCommon(
                episode      = episode,
                root         = binding.root,
                tvTitle      = binding.tvEpisodeTitle,
                tvDuration   = binding.tvEpisodeDuration,
                tvDesc       = binding.tvEpisodeDescription,
                tvRating     = binding.tvEpisodeRating,
                ivThumbnail  = binding.ivEpisodeThumbnail,
                ivPlay       = binding.ivPlay,
                tvNewBadge   = null
            )
        }
    }

    // ── ViewHolder "New" ─────────────────────────────────────────────────────

    inner class NewViewHolder(
        private val binding: ItemEpisodeNewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(episode: Channel) {
            bindCommon(
                episode      = episode,
                root         = binding.root,
                tvTitle      = binding.tvEpisodeTitle,
                tvDuration   = binding.tvEpisodeDuration,
                tvDesc       = binding.tvEpisodeDescription,
                tvRating     = binding.tvEpisodeRating,
                ivThumbnail  = binding.ivEpisodeThumbnail,
                ivPlay       = binding.ivPlay,
                tvNewBadge   = binding.tvNewBadge
            )
        }
    }

    // ── Logic bind dùng chung ─────────────────────────────────────────────────

    private fun bindCommon(
        episode: Channel,
        root: View,
        tvTitle: TextView,
        tvDuration: TextView,
        tvDesc: TextView,
        tvRating: TextView,
        ivThumbnail: ImageView,
        ivPlay: ImageView,
        tvNewBadge: TextView?
    ) {
        // Tên tập
        val cleanTitle = episode.name
            .substringAfter(" - ")
            .substringAfter(" - ")
            .ifBlank { episode.name }
        tvTitle.text = cleanTitle

        // Thời lượng
        tvDuration.text = episode.episodeDuration?.let { "$it min" } ?: ""

        // Mô tả
        val desc = episode.description.orEmpty()
        if (desc.isNotBlank()) {
            tvDesc.visibility = View.VISIBLE
            tvDesc.text = desc
        } else {
            tvDesc.visibility = View.GONE
        }

        // Rating
        val rating = episode.rating
        if (rating != null && rating > 0f) {
            tvRating.visibility = View.VISIBLE
            tvRating.text = "★ ${String.format("%.1f", rating)}"
        } else {
            tvRating.visibility = View.GONE
        }

        // Badge "New" (chỉ có ở item_episode_new)
        tvNewBadge?.visibility = View.VISIBLE

        // Thumbnail
        Glide.with(root.context)
            .load(episode.logo.ifBlank { null })
            .placeholder(R.drawable.ic_tv_placeholder)
            .error(R.drawable.ic_tv_placeholder)
            .centerCrop()
            .into(ivThumbnail)

        root.setOnClickListener { onEpisodeClick(episode) }
        ivPlay.setOnClickListener { onEpisodeClick(episode) }
    }

    class DiffCallback : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(a: Channel, b: Channel) = a.id == b.id
        override fun areContentsTheSame(a: Channel, b: Channel) = a == b
    }
}