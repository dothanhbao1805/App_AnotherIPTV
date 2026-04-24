package com.example.anotheriptv.presentation.xstream.live

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentLiveXstreamBinding
import com.example.anotheriptv.presentation.channels.Adapter.CategoryAdapter
import com.example.anotheriptv.presentation.player.PlayerActivity
import com.example.anotheriptv.presentation.xstream.live.ViewModelFactory.LiveXstreamViewModelFactory
import com.example.anotheriptv.presentation.xstream.live.ViewModel.LiveXstreamViewModel
import kotlinx.coroutines.launch

class LiveXstreamFragment : Fragment() {

    private var _binding: FragmentLiveXstreamBinding? = null
    private val binding get() = _binding!!
    private var playlistId: Long = -1L

    private lateinit var categoryAdapter: CategoryAdapter

    private val viewModel: LiveXstreamViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        LiveXstreamViewModelFactory(
            container.channelRepository,
            container.categoryDao,
            container.addWatchHistoryUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveXstreamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = arguments?.getLong("playlistId") ?: -1L

        setupRecyclerView()
        observeViewModel()

        viewModel.loadAllLive(playlistId)
        viewModel.loadLiveChannels(playlistId)

        binding.ivSearch.setOnClickListener {
            val fragment = SearchLiveXstreamFragment.newInstance(playlistId)
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
                viewModel.addToHistory(
                    channelId   = channel.id,
                    playlistId  = playlistId,
                    channelName = channel.name,
                    channelLogo = channel.logo
                )
                val intent = android.content.Intent(requireContext(), PlayerActivity::class.java).apply {
                    putExtra("channelName", channel.name)
                    putExtra("streamUrl", channel.url)
                }
                startActivity(intent)
            },
            onViewAllClick = { category ->
                val bundle = Bundle().apply {
                    putLong("playlistId", playlistId)
                    putString("contentType", "LIVE")
                    putString("categoryId", category.categoryId)
                    putString("categoryName",category.categoryName)
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LiveXstreamAllFragment().apply { arguments = bundle })
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