package com.example.anotheriptv.presentation.playlist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.example.anotheriptv.R
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.databinding.FragmentPlaylistBinding
import com.example.anotheriptv.presentation.channels.ChannelFragment
import com.example.anotheriptv.presentation.playlist.Adapter.PlaylistAdapter
import com.example.anotheriptv.presentation.playlist.UiState.PlaylistUiState
import com.example.anotheriptv.presentation.playlist.ViewModel.PlaylistViewModel
import com.example.anotheriptv.presentation.playlist.ViewModelFactory.PlaylistViewModelFactory
import kotlinx.coroutines.launch

class PlaylistFragment : Fragment() {

    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    private lateinit var playlistAdapter: PlaylistAdapter

    val viewModel: PlaylistViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        PlaylistViewModelFactory(
            container.getPlaylistsUseCase,
            container.addPlaylistUseCase,
            container.deletePlaylistUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerPlaylists.visibility = View.GONE

        setupRecyclerView()
        observeViewModel()

        binding.fabAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NewPlaylistFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnCreateFirst.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NewPlaylistFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupRecyclerView() {
        playlistAdapter = PlaylistAdapter(
            onPlaylistClick = { playlist ->
                val fragment = ChannelFragment().apply {
                    arguments = Bundle().apply {
                        putLong("playlistId", playlist.id)
                        putString("playlistName", playlist.name)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { playlist ->
                viewModel.deletePlaylist(playlist.id)
            }
        )
        binding.recyclerPlaylists.apply {
            adapter = playlistAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.playlists.collect { playlists ->
                        if (playlists.isEmpty()) {
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.recyclerPlaylists.visibility = View.GONE
                        } else {
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.recyclerPlaylists.visibility = View.VISIBLE
                            Log.d("DEBUG", "collect playlists size = ${playlists.size}")
                            playlistAdapter.submitList(playlists)
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is PlaylistUiState.Loading -> { }
                            is PlaylistUiState.Success -> {
                                viewModel.resetState()
                                parentFragmentManager.popBackStack()
                            }
                            is PlaylistUiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetState()
                            }
                            is PlaylistUiState.Idle -> { }
                        }
                    }
                }
            }
        }
    }

    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerPlaylists.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}