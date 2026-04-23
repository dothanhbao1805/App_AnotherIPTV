package com.example.anotheriptv.domain.usecase.channel

import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow

class GetChannelsByCategoryUseCase(
    private val channelRepository: ChannelRepository
) {
    operator fun invoke(playlistId: Long, contentType:String, categoryId:String): Flow<List<Channel>> {
        return channelRepository.getChannelsByCategory(playlistId,contentType,categoryId)
    }
}