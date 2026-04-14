package com.example.anotheriptv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String,           // "M3U" | "XSTREAM"
    val createdAt: Long,

    // ── XStream only ──
    val url: String? = null,
    val userName: String? = null,
    val password: String? = null,

    // ── M3U only ──
    val sourceType: String? = null, // "URL" | "FILE"
    val m3uUrl: String? = null,     // nếu sourceType = URL
    val filePath: String? = null    // nếu sourceType = FILE (đường dẫn local)
)