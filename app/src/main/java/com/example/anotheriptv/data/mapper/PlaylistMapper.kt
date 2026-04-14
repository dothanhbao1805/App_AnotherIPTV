package com.example.anotheriptv.data.mapper

import com.example.anotheriptv.data.local.entity.PlaylistEntity
import com.example.anotheriptv.domain.model.Playlist

class PlaylistMapper {

    fun toDomain(entity: PlaylistEntity): Playlist {
        return Playlist(
            id         = entity.id,
            name       = entity.name,
            type       = entity.type,
            createdAt  = entity.createdAt,
            url        = entity.url,
            userName   = entity.userName,
            password   = entity.password,
            sourceType = entity.sourceType,
            m3uUrl     = entity.m3uUrl,
            filePath   = entity.filePath
        )
    }

    fun toEntity(domain: Playlist): PlaylistEntity {
        return PlaylistEntity(
            id         = domain.id,
            name       = domain.name,
            type       = domain.type,
            createdAt  = domain.createdAt,
            url        = domain.url,
            userName   = domain.userName,
            password   = domain.password,
            sourceType = domain.sourceType,
            m3uUrl     = domain.m3uUrl,
            filePath   = domain.filePath
        )
    }
}