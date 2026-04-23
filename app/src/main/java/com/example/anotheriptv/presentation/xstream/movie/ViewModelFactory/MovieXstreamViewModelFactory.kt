package com.example.anotheriptv.presentation.xstream.movie.ViewModelFactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.anotheriptv.data.local.dao.CategoryDao
import com.example.anotheriptv.domain.repository.ChannelRepository
import com.example.anotheriptv.domain.usecase.history.AddWatchHistoryUseCase
import com.example.anotheriptv.presentation.xstream.movie.ViewModel.MovieXstreamViewModel

class MovieXstreamViewModelFactory(
    private val channelRepository: ChannelRepository,
    private val categoryDao: CategoryDao,
    private val addWatchHistoryUseCase: AddWatchHistoryUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovieXstreamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MovieXstreamViewModel(
                channelRepository,
                categoryDao,
                addWatchHistoryUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}