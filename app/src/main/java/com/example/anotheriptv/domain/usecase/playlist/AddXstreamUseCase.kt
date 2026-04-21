package com.example.anotheriptv.domain.usecase.playlist

import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.domain.repository.PlaylistRepository

class AddXstreamUseCase(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(
        playlist: Playlist,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Result<Long> {
        return try {
            validate(playlist)
            val id = playlistRepository.addPlaylistXstream(playlist, onProgress)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validate(playlist: Playlist) {
        when (playlist.type) {
            "XSTREAM" -> {
                require(playlist.name.isNotBlank()) { "Tên playlist không được trống" }
                require(!playlist.url.isNullOrBlank()) { "URL không được trống" }
                require(!playlist.userName.isNullOrBlank()) { "Username không được trống" }
                require(!playlist.password.isNullOrBlank()) { "Password không được trống" }
            }
        }
    }

}