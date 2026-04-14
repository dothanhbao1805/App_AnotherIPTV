package com.example.anotheriptv.domain.usecase.playlist

import com.example.anotheriptv.domain.repository.PlaylistRepository

class DeletePlaylistUseCase(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: Long): Result<Unit> {
        return try {
            playlistRepository.deletePlaylist(playlistId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}