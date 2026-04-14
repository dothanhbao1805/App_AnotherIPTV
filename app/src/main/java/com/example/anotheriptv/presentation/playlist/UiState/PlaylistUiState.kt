package com.example.anotheriptv.presentation.playlist.UiState


sealed class PlaylistUiState {
    object Idle : PlaylistUiState()
    object Loading : PlaylistUiState()
    object Success : PlaylistUiState()
    data class Error(val message: String) : PlaylistUiState()
}