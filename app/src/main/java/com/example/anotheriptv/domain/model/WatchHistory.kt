package com.example.anotheriptv.domain.model

data class WatchHistory(
    val id: Long = 0,
    val channelId: Long,
    val playlistId: Long,
    val channelName: String,
    val channelLogo: String,
    val streamUrl: String?, // Bắt buộc phải có để truyền sang Player
    val watchedAt: Long
)