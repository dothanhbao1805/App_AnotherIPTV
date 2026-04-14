package com.example.anotheriptv.domain.model

data class WatchHistory(
    val id: Long = 0,
    val channelId: Long,
    val channelName: String,
    val channelLogo: String,
    val watchedAt: Long
)