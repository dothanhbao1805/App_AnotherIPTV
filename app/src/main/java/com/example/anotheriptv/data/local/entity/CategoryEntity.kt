package com.example.anotheriptv.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "categories",
    primaryKeys = ["playlistId", "categoryId", "contentType"]
)

data class CategoryEntity(
    val playlistId: Long,
    val categoryId: String,
    val contentType: String,
    val name: String
)