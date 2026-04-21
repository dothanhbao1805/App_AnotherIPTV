package com.example.anotheriptv.domain.model

data class Channel(
    val id: Long = 0,
    val playlistId: Long,
    val contentType: String = "LIVE",
    val name: String,
    val url: String,
    val logo: String,
    val category: String,
    val isFavorite: Boolean = false,

    // Movie + Series
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