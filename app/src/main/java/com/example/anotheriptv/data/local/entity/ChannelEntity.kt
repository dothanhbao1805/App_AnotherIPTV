package com.example.anotheriptv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,     // FK → PlaylistEntity
    val name: String,
    val url: String,
    val category: String,
    val logo: String,
    val isFavorite: Boolean = false
)