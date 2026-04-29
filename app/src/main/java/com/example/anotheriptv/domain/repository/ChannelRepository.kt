package com.example.anotheriptv.domain.repository

import com.example.anotheriptv.data.local.entity.ChannelEntity
import com.example.anotheriptv.domain.model.CategoryWithChannels
import com.example.anotheriptv.domain.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getChannelsByPlaylistId(playlistId: Long): Flow<List<Channel>>
    fun getFavoriteChannels(): Flow<List<Channel>>
    suspend fun insertChannels(channels: List<Channel>)
    suspend fun toggleFavorite(channelId: Long, isFavorite: Boolean)
    suspend fun deleteChannelsByPlaylistId(playlistId: Long)
    suspend fun getChannelsByCategoryLimit10(
        playlistId: Long,
        contentType: String,
        categoryId: String
    ): List<Channel>

    fun getChannelsByCategory(
        playlistId: Long,
        contentType: String,
        categoryId: String
    ):Flow<List<Channel>>

    fun getChannelsByContentType(playlistId: Long, contentType: String): Flow<List<Channel>>

    fun getEpisodesBySeriesId(playlistId: Long, seriesId: Long): Flow<List<Channel>>

    suspend fun getChannelByUrl(url: String): Channel?
    suspend fun updateFavoriteStatus(url: String, isFavorite: Boolean)

    suspend fun getAllChannels(): List<Channel>
    suspend fun getAllCategoriesWithChannels(playlistId: Long, contentType: String): List<CategoryWithChannels>

    suspend fun getChannelById(channelId: Long): Channel?

}