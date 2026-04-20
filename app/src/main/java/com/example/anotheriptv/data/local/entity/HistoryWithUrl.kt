package com.example.anotheriptv.data.local.entity

data class HistoryWithUrl(
    val historyId: Long,
    val channelId: Long,
    val playlistId: Long,
    val channelName: String,
    val channelLogo: String,
    val streamUrl: String?, // Lấy từ bảng channels
    val watchedAt: Long
)