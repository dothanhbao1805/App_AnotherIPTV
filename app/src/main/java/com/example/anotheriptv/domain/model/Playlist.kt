package com.example.anotheriptv.domain.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val type: String,           // "M3U" | "XSTREAM"
    val createdAt: Long,

    // XStream
    val url: String? = null,
    val userName: String? = null,
    val password: String? = null,

    // M3U
    val sourceType: String? = null,
    val m3uUrl: String? = null,
    val filePath: String? = null
)