package com.example.anotheriptv.presentation.xstream.series


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.databinding.FragmentSeriesXstreamBinding
import com.example.anotheriptv.presentation.xstream.series.Adapter.CategoryAdapter
import com.example.anotheriptv.presentation.xstream.series.ViewModelFactory.SeriesXstreamViewModelFactory
import com.example.anotheriptv.presentation.xstream.series.ViewModel.SeriesXstreamViewModel
import kotlinx.coroutines.launch

class SeriesXstreamFragment : Fragment() {

    private var _binding: FragmentSeriesXstreamBinding? = null
    private val binding get() = _binding!!
    private var playlistId: Long = -1L

    private lateinit var categoryAdapter: CategoryAdapter

    private val viewModel: SeriesXstreamViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        SeriesXstreamViewModelFactory(
            container.channelRepository,
            container.categoryDao,
            container.addWatchHistoryUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeriesXstreamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = arguments?.getLong("playlistId") ?: -1L

        setupRecyclerView()
        observeViewModel()
        viewModel.loadAllSeries(playlistId)
        viewModel.loadLiveChannels(playlistId)

        binding.ivSearch.setOnClickListener {
        }

    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onChannelClick = { channel ->

            },

            onViewAllClick = { category ->
            }
        )

        binding.recyclerLive.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categoriesWithChannels.collect { categories ->
                    categoryAdapter.submitList(categories)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}