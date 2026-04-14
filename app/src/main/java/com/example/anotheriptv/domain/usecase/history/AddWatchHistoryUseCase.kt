package com.example.anotheriptv.domain.usecase.history

import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.domain.model.WatchHistory
import com.example.anotheriptv.domain.repository.WatchHistoryRepository

class AddWatchHistoryUseCase(
    private val historyRepository: WatchHistoryRepository
) {
    suspend operator fun invoke(channel: Channel): Result<Unit> {
        return try {
            val history = WatchHistory(
                channelId = channel.id,
                channelName = channel.name,
                channelLogo = channel.logo,
                watchedAt = System.currentTimeMillis()
            )
            historyRepository.addHistory(history)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}