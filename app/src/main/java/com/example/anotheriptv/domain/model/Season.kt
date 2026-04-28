package com.example.anotheriptv.domain.model

data class Season(
    val seasonNumber: Int,
    val name: String,           // "Season 1", "Season 2", ...
    val episodeCount: Int = 0 , // số tập trong season này
    val releaseDate: String? = null
)