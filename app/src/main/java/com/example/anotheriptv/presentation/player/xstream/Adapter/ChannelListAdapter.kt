package com.example.anotheriptv.presentation.player.xstream.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anotheriptv.R
import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.databinding.ItemChannelListBinding

class ChannelListAdapter(
    private val items: List<Channel>,
    private var currentUrl: String,
    private val onClick: (Channel) -> Unit  // ← đổi từ ChannelEntity sang Channel
) : RecyclerView.Adapter<ChannelListAdapter.VH>() {

    inner class VH(val binding: ItemChannelListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemChannelListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val channel = items[position]
        holder.binding.tvChannelName.text   = channel.name
        holder.binding.tvChannelNumber.text = "#${position + 1}"

        Glide.with(holder.itemView)
            .load(channel.logo)  // ← dùng đúng field name của Channel
            .placeholder(R.drawable.ic_tv2)
            .into(holder.binding.ivChannelLogo)

        if (channel.url == currentUrl) {
            holder.binding.tvChannelName.setTextColor(0xFF2196F3.toInt())
        } else {
            holder.binding.tvChannelName.setTextColor(0xFFFFFFFF.toInt())
        }

        holder.binding.root.setOnClickListener { onClick(channel) }
    }

    fun updateCurrentUrl(newUrl: String) {
        currentUrl = newUrl
        notifyDataSetChanged()
    }

}