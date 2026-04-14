package com.example.anotheriptv.data.mapper

import com.example.anotheriptv.data.local.entity.WatchHistoryEntity
import com.example.anotheriptv.domain.model.WatchHistory

class HistoryMapper {

    fun toDomain(entity: WatchHistoryEntity): WatchHistory {
        return WatchHistory(
            id          = entity.id,
            channelId   = entity.channelId,
            channelName = entity.channelName,
            channelLogo = entity.channelLogo,
            watchedAt   = entity.watchedAt
        )
    }

    fun toEntity(domain: WatchHistory): WatchHistoryEntity {
        return WatchHistoryEntity(
            id          = domain.id,
            channelId   = domain.channelId,
            channelName = domain.channelName,
            channelLogo = domain.channelLogo,
            watchedAt   = domain.watchedAt
        )
    }
}