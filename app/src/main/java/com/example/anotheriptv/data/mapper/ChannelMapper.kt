package com.example.anotheriptv.data.mapper

import com.example.anotheriptv.data.local.entity.ChannelEntity
import com.example.anotheriptv.domain.model.Channel

class ChannelMapper {

    fun toDomain(entity: ChannelEntity): Channel {
        return Channel(
            id         = entity.id,
            playlistId = entity.playlistId,
            name       = entity.name,
            url        = entity.url,
            category   = entity.category,
            logo       = entity.logo,
            isFavorite = entity.isFavorite
        )
    }

    fun toEntity(domain: Channel): ChannelEntity {
        return ChannelEntity(
            id         = domain.id,
            playlistId = domain.playlistId,
            name       = domain.name,
            url        = domain.url,
            category   = domain.category,
            logo       = domain.logo,
            isFavorite = domain.isFavorite
        )
    }

    fun fromParsed(
        playlistId: Long,
        name: String,
        url: String,
        category: String,
        logo: String
    ): ChannelEntity {
        return ChannelEntity(
            playlistId = playlistId,
            name       = name,
            url        = url,
            category   = category,
            logo       = logo
        )
    }
}