package com.example.anotheriptv.domain.repository

import com.example.anotheriptv.domain.model.Playlist
import kotlinx.coroutines.flow.Flow


interface PlaylistRepository {
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylistById(id: Long): Playlist?
    suspend fun addPlaylist(playlist: Playlist): Long

    suspend fun addPlaylistXstream(
        playlist: Playlist,
        onProgress: (progress: Int, status: String) -> Unit = { _, _ -> }
    ): Long
    suspend fun deletePlaylist(id: Long)
}