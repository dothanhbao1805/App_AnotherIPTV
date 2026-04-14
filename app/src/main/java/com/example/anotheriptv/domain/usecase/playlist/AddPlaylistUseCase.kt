package com.example.anotheriptv.domain.usecase.playlist

import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.domain.repository.PlaylistRepository

class AddPlaylistUseCase(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(playlist: Playlist): Result<Long> {
        return try {
            // Validate trước khi thêm
            validate(playlist)
            val id = playlistRepository.addPlaylist(playlist)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validate(playlist: Playlist) {
        when (playlist.type) {
            "XSTREAM" -> {
                require(playlist.name.isNotBlank())  { "Tên playlist không được trống" }
                require(!playlist.url.isNullOrBlank()) { "URL không được trống" }
                require(!playlist.userName.isNullOrBlank()) { "Username không được trống" }
                require(!playlist.password.isNullOrBlank()) { "Password không được trống" }
            }
            "M3U" -> {
                require(playlist.name.isNotBlank()) { "Tên playlist không được trống" }
                when (playlist.sourceType) {
                    "URL"  -> require(!playlist.m3uUrl.isNullOrBlank()) { "URL không được trống" }
                    "FILE" -> require(!playlist.filePath.isNullOrBlank()) { "File không được trống" }
                }
            }
        }
    }
}