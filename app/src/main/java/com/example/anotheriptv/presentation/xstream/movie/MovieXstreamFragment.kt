package com.example.anotheriptv.presentation.xstream.movie

import android.content.Intent
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
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentMovieXstreamBinding
import com.example.anotheriptv.presentation.xstream.movie.Adapter.CategoryAdapter
import com.example.anotheriptv.presentation.xstream.movie.ViewModelFactory.MovieXstreamViewModelFactory
import com.example.anotheriptv.presentation.xstream.movie.ViewModel.MovieXstreamViewModel
import kotlinx.coroutines.launch

class MovieXstreamFragment : Fragment() {

    private var _binding: FragmentMovieXstreamBinding? = null
    private val binding get() = _binding!!
    private var playlistId: Long = -1L

    private lateinit var categoryAdapter: CategoryAdapter

    private val viewModel: MovieXstreamViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        MovieXstreamViewModelFactory(
            container.channelRepository,
            container.categoryDao,
            container.addWatchHistoryUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieXstreamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = arguments?.getLong("playlistId") ?: -1L

        setupRecyclerView()
        observeViewModel()
        viewModel.loadAllMovies(playlistId)
        viewModel.loadLiveChannels(playlistId)

        binding.ivSearch.setOnClickListener {
            val fragment = SearchMovieFragment.newInstance(playlistId)
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onChannelClick = { channel ->

                // Mở DetailMovieActivity
                val intent = Intent(requireContext(), DetailMovieActivity::class.java).apply {
                    putExtra(DetailMovieActivity.EXTRA_LOGO,         channel.logo)
                    putExtra(DetailMovieActivity.EXTRA_NAME,         channel.name)
                    putExtra(DetailMovieActivity.EXTRA_RATING,       channel.rating?.toFloat() ?: 0f)
                    putExtra(DetailMovieActivity.EXTRA_RELEASE_DATE, channel.releaseDate.orEmpty())
                    putExtra(DetailMovieActivity.EXTRA_STREAM_URL,   channel.url)
                    putExtra(DetailMovieActivity.EXTRA_CHANNEL_ID,   channel.id)
                    putExtra(DetailMovieActivity.EXTRA_PLAYLIST_ID,  playlistId)
                }
                startActivity(intent)
            },

            onViewAllClick = { category ->
                val bundle = Bundle().apply {
                    putLong("playlistId", playlistId)
                    putString("contentType", "MOVIE")
                    putString("categoryId", category.categoryId)
                    putString("categoryName", category.categoryName)
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, MovieXstreamAllFragment().apply { arguments = bundle })
                    .addToBackStack(null)
                    .commit()
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