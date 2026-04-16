package com.example.anotheriptv.data.mapper

import com.example.anotheriptv.data.local.entity.HistoryWithUrl
import com.example.anotheriptv.data.local.entity.WatchHistoryEntity
import com.example.anotheriptv.domain.model.WatchHistory

class HistoryMapper {

    fun toDomain(entity: HistoryWithUrl): WatchHistory {
        return WatchHistory(
            id          = entity.historyId,
            channelId   = entity.channelId,
            channelName = entity.channelName,
            channelLogo = entity.channelLogo,
            streamUrl = entity.streamUrl,
            watchedAt   = entity.watchedAt
        )
    }

    fun toEntity(domain: WatchHistory): WatchHistoryEntity {
        return WatchHistoryEntity(
            id = domain.id,
            channelId = domain.channelId,
            channelName = domain.channelName,
            channelLogo = domain.channelLogo,
            watchedAt = domain.watchedAt
        )
    }

}