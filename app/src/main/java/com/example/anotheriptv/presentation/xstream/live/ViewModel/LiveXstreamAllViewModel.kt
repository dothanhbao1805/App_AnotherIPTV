package com.example.anotheriptv.presentation.xstream.live.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.domain.usecase.channel.GetChannelsByCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LiveXstreamAllViewModel(
    private val getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase
) : ViewModel() {

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())
    private val _channels    = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadChannels(playlistId: Long, contentType: String, categoryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            getChannelsByCategoryUseCase(playlistId, contentType, categoryId)
                .collectLatest { list ->
                    _allChannels.value = list
                    _channels.value    = list
                    _isLoading.value   = false
                }
        }
    }

    fun search(query: String) {
        _channels.value = if (query.isBlank()) {
            _allChannels.value
        } else {
            _allChannels.value.filter {
                it.name.contains(query, ignoreCase = true)
            }
        }
    }

    fun clearSearch() {
        _channels.value = _allChannels.value
    }
}

class LiveXstreamAllViewModelFactory(
    private val getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LiveXstreamAllViewModel(getChannelsByCategoryUseCase) as T
    }
}

