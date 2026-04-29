package com.example.anotheriptv.presentation.xstream.series

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.BottomSheetEpisodesBinding
import com.example.anotheriptv.presentation.player.m3u.PlayerActivity
import com.example.anotheriptv.presentation.xstream.series.Adapter.EpisodeAdapter
import com.example.anotheriptv.presentation.xstream.series.ViewModel.SeriesXstreamViewModel
import com.example.anotheriptv.presentation.xstream.series.ViewModelFactory.SeriesXstreamViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class EpisodesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEpisodesBinding? = null
    private val binding get() = _binding!!

    private val playlistId:   Long by lazy { arguments?.getLong(ARG_PLAYLIST_ID, -1L) ?: -1L }
    private val seriesId:     Long by lazy { arguments?.getLong(ARG_SERIES_ID, -1L) ?: -1L }
    private val seasonNumber: Int  by lazy { arguments?.getInt(ARG_SEASON_NUMBER, 1) ?: 1 }
    private val seasonName:   String by lazy { arguments?.getString(ARG_SEASON_NAME, "Season") ?: "Season" }

    private val viewModel: SeriesXstreamViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        SeriesXstreamViewModelFactory(
            container.channelRepository,
            container.categoryDao,
            container.addWatchHistoryUseCase,
            container.channelDao,
            container.xstreamParser
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEpisodesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSeasonTitle.text = seasonName

        val episodeAdapter = EpisodeAdapter { episode ->
            startActivity(Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("channelName", episode.name)
                putExtra("streamUrl",   episode.url)
            })
        }

        binding.recyclerEpisodes.apply {
            adapter       = episodeAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.episodes.collect { episodes ->
                    episodeAdapter.submitList(episodes)
                    binding.tvEpisodeCount.text = "${episodes.size} Episodes"
                }
            }
        }

        viewModel.loadEpisodes(playlistId, seriesId, seasonNumber)
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_PLAYLIST_ID   = "arg_playlist_id"
        const val ARG_SERIES_ID     = "arg_series_id"
        const val ARG_SEASON_NUMBER = "arg_season_number"
        const val ARG_SEASON_NAME   = "arg_season_name"

        fun newInstance(
            playlistId: Long, seriesId: Long,
            seasonNumber: Int, seasonName: String
        ) = EpisodesBottomSheet().apply {
            arguments = Bundle().apply {
                putLong(ARG_PLAYLIST_ID,   playlistId)
                putLong(ARG_SERIES_ID,     seriesId)
                putInt(ARG_SEASON_NUMBER,  seasonNumber)
                putString(ARG_SEASON_NAME, seasonName)
            }
        }
    }

}