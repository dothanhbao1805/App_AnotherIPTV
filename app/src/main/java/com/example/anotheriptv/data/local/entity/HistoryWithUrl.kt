package com.example.anotheriptv.data.local.entity

data class HistoryWithUrl(
    val historyId: Long,
    val channelId: Long,
    val playlistId: Long,
    val channelName: String,
    val channelLogo: String,
    val streamUrl: String?,
    val watchedAt: Long,
    val contentType: String
)