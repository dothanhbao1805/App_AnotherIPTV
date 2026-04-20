package com.example.anotheriptv.data.repository

import com.example.anotheriptv.data.local.dao.WatchHistoryDao
import com.example.anotheriptv.data.mapper.HistoryMapper
import com.example.anotheriptv.domain.model.WatchHistory
import com.example.anotheriptv.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WatchHistoryRepositoryImpl(
    private val historyDao: WatchHistoryDao,
    private val historyMapper: HistoryMapper
) : WatchHistoryRepository {

    override fun getWatchHistory(): Flow<List<WatchHistory>> {
        return historyDao.getAll()
            .map { list -> list.map { historyMapper.toDomain(it) } }
    }

    override fun getWatchHistoryByPlaylist(playlistId: Long): Flow<List<WatchHistory>> {
        return historyDao.getByPlaylistId(playlistId)
            .map { list -> list.map { historyMapper.toDomain(it) } }
    }

    override suspend fun upsertHistory(watchHistory: WatchHistory) {
        historyDao.deleteExisting(watchHistory.channelId, watchHistory.playlistId)
        historyDao.insert(historyMapper.toEntity(watchHistory))
    }

    override suspend fun deleteHistoryById(id: Long) {
        historyDao.deleteById(id)
    }

}