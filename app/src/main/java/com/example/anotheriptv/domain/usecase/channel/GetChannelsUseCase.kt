package com.example.anotheriptv.domain.usecase.channel

import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow

class GetChannelsUseCase(
    private val channelRepository: ChannelRepository
) {
    operator fun invoke(playlistId: Long): Flow<List<Channel>> {
        return channelRepository.getChannelsByPlaylistId(playlistId)
    }
}