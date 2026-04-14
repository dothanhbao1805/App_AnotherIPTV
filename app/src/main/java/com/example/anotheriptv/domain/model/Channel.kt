package com.example.anotheriptv.domain.model

data class Channel(
    val id: Long = 0,
    val playlistId: Long,
    val name: String,
    val url: String,
    val category: String,
    val logo: String,
    val isFavorite: Boolean = false
)