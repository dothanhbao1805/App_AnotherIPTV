package com.example.anotheriptv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channelId: Long,      // FK → ChannelEntity
    val channelName: String,
    val channelLogo: String,
    val watchedAt: Long
)