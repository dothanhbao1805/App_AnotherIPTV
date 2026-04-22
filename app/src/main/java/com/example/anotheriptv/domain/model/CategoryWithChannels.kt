package com.example.anotheriptv.domain.model

data class CategoryWithChannels(
    val categoryId: String,
    val categoryName: String,
    val channels: List<Channel>
)