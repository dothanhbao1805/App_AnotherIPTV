package com.example.anotheriptv.domain.usecase.history

import com.example.anotheriptv.domain.model.WatchHistory
import com.example.anotheriptv.domain.repository.WatchHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetWatchHistoryUseCase(private val repository: WatchHistoryRepository) {
    operator fun invoke(playlistId: Long): Flow<List<WatchHistory>> {
        return repository.getWatchHistoryByPlaylist(playlistId)
    }
}