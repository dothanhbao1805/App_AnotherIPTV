package com.example.anotheriptv.data.repository

import com.example.anotheriptv.data.local.dao.ChannelDao
import com.example.anotheriptv.data.local.entity.ChannelEntity
import com.example.anotheriptv.data.mapper.ChannelMapper
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

    override fun getFavoriteChannels(): Flow<List<Channel>> {
        return channelDao.getFavorites()
            .map { entities -> entities.map { channelMapper.toDomain(it) } }
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

}