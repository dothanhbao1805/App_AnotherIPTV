package com.example.anotheriptv.data.repository

import com.example.anotheriptv.data.local.dao.ChannelDao
import com.example.anotheriptv.data.local.entity.ChannelEntity
import com.example.anotheriptv.data.mapper.ChannelMapper
import com.example.anotheriptv.domain.model.CategoryWithChannels
import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class ChannelRepositoryImpl(
    private val channelDao: ChannelDao,
    private val channelMapper: ChannelMapper
) : ChannelRepository {

    override fun getChannelsByPlaylistId(playlistId: Long): Flow<List<Channel>> {
        return channelDao.getByPlaylistId(playlistId)
            .map { entities -> entities.map { channelMapper.toDomain(it) } }
    }

    override fun getFavoriteChannels(playlistId: Long): Flow<List<Channel>> =
        channelDao.getFavoriteChannels(playlistId).map { entities ->
            entities.map { channelMapper.toDomain(it) }
        }

    override suspend fun insertChannels(channels: List<Channel>) {
        channelDao.insertAll(channels.map { channelMapper.toEntity(it) })
    }

    override suspend fun toggleFavorite(channelId: Long, isFavorite: Boolean) {
        channelDao.updateFavorite(channelId, isFavorite)
    }

    override suspend fun deleteChannelsByPlaylistId(playlistId: Long) {
        channelDao.deleteByPlaylistId(playlistId)
    }

    override suspend fun getChannelsByCategoryLimit10(
        playlistId: Long,
        contentType: String,
        categoryId: String
    ): List<Channel> {
        return channelDao.getChannelsByCategoryLimit10(playlistId, contentType, categoryId)
            .map { channelMapper.toDomain(it) }
    }

    override fun getChannelsByCategory(
        playlistId: Long,
        contentType: String,
        categoryId: String
    ): Flow<List<Channel>> {
        return channelDao.getChannelsByCategory(playlistId, contentType, categoryId)
            .map { list -> list.map { channelMapper.toDomain(it) } }
    }

    override fun getChannelsByContentType(playlistId: Long, contentType: String): Flow<List<Channel>> {
        return channelDao.getByPlaylistIdAndContentType(playlistId, contentType)
            .map { entities -> entities.map { channelMapper.toDomain(it) } }
    }

    override fun getEpisodesBySeriesId(playlistId: Long, seriesId: Long): Flow<List<Channel>> {
        return channelDao.getEpisodesBySeriesId(playlistId, seriesId)
            .map { entities -> entities.map { channelMapper.toDomain(it) } }
    }

    override suspend fun getChannelByUrl(url: String): Channel? {
        val entity = channelDao.getChannelByUrl(url)
        return entity?.let { channelMapper.toDomain(it) }
    }

    override suspend fun updateFavoriteStatus(url: String, isFavorite: Boolean) {
        channelDao.updateFavoriteStatus(url, isFavorite)
    }

    override suspend fun getAllChannels(): List<Channel> {
        return channelDao.getAllChannels().map { channelMapper.toDomain(it) }
    }

    override suspend fun getAllCategoriesWithChannels(
        playlistId: Long,
        contentType: String
    ): List<CategoryWithChannels> {
        val allChannels = channelDao.getAllChannelsByPlaylistAndType(playlistId, contentType)
            .map { channelMapper.toDomain(it) }

        return allChannels
            .groupBy { it.category ?: "Unknown" }
            .map { (categoryId, channels) ->
                CategoryWithChannels(
                    categoryId   = categoryId,
                    categoryName = channels.firstOrNull()?.category ?: categoryId,
                    channels     = channels
                )
            }
            .sortedBy { it.categoryName }
    }

    override suspend fun getChannelById(channelId: Long): Channel? {
        val entity = channelDao.getChannelById(channelId)
        return entity?.let { channelMapper.toDomain(it) }
    }


}