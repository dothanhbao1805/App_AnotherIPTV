package com.example.anotheriptv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val contentType: String,    // "LIVE" | "MOVIE" | "SERIES"
    val name: String,
    val url: String,
    val logo: String,
    val category: String,       // tên category đã resolve
    val categoryId: String = "", // id gốc từ API để filter
    val isFavorite: Boolean = false,

    // Movie + Series only
    val description: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val cast: String? = null,
    val trailerUrl: String? = null,
    val rating: Float? = null,

    // Series only
    val seriesId: Long? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeDuration: Int? = null
)