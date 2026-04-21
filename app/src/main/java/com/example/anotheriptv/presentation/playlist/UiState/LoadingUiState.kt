package com.example.anotheriptv.presentation.playlist.UiState

import com.example.anotheriptv.domain.model.Playlist

sealed class LoadingUiState {
    object Idle : LoadingUiState()
    data class Loading(
        val progress: Int = 0,          // 0-100
        val statusText: String = ""     // "Loading live channels..."
    ) : LoadingUiState()

    data class Success(val playlist: Playlist? = null) : LoadingUiState()

    data class Error(val message: String) : LoadingUiState()
}