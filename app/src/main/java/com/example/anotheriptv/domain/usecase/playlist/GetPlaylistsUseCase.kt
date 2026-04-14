package com.example.anotheriptv.domain.usecase.playlist

import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow

class GetPlaylistsUseCase(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(): Flow<List<Playlist>> {
        return playlistRepository.getPlaylists()
    }
}