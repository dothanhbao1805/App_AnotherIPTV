package com.example.anotheriptv.data.mapper

import com.example.anotheriptv.data.local.entity.ChannelEntity
import com.example.anotheriptv.domain.model.Channel

class ChannelMapper {

    fun toDomain(entity: ChannelEntity): Channel {
        return Channel(
            id          = entity.id,
            playlistId  = entity.playlistId,
            contentType = entity.contentType,
            name        = entity.name,
            url         = entity.url,
            logo        = entity.logo,
            category    = entity.category,
            isFavorite  = entity.isFavorite,
            description = entity.description,
            genre       = entity.genre,
            releaseDate = entity.releaseDate,
            cast        = entity.cast,
            trailerUrl  = entity.trailerUrl,
            rating      = entity.rating,
            seriesId    = entity.seriesId,
            seasonNumber    = entity.seasonNumber,
            episodeNumber   = entity.episodeNumber,
            episodeDuration = entity.episodeDuration
        )
    }

    fun toEntity(domain: Channel): ChannelEntity {
        return ChannelEntity(
            id          = domain.id,
            playlistId  = domain.playlistId,
            contentType = domain.contentType,
            name        = domain.name,
            url         = domain.url,
            logo        = domain.logo,
            category    = domain.category,
            isFavorite  = domain.isFavorite,
            description = domain.description,
            genre       = domain.genre,
            releaseDate = domain.releaseDate,
            cast        = domain.cast,
            trailerUrl  = domain.trailerUrl,
            rating      = domain.rating,
            seriesId    = domain.seriesId,
            seasonNumber    = domain.seasonNumber,
            episodeNumber   = domain.episodeNumber,
            episodeDuration = domain.episodeDuration
        )
    }

    fun fromParsed(
        playlistId: Long,
        name: String,
        url: String,
        category: String,
        logo: String,
        contentType: String = "LIVE"
    ): ChannelEntity {
        return ChannelEntity(
            playlistId = playlistId,
            contentType = contentType,
            name       = name,
            url        = url,
            category   = category,
            logo       = logo
        )
    }

}