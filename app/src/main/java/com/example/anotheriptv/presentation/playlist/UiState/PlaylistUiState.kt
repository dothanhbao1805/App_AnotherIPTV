package com.example.anotheriptv.presentation.playlist.UiState

import com.example.anotheriptv.domain.model.Playlist

sealed class PlaylistUiState {
    object Idle : PlaylistUiState()
    object Loading : PlaylistUiState()
    // Cho phép playlist có thể null để dùng cho trường hợp Xóa (Delete)
    data class Success(val playlist: Playlist? = null) : PlaylistUiState()
    data class Error(val message: String) : PlaylistUiState()
}