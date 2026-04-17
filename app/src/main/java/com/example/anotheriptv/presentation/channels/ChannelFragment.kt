package com.example.anotheriptv.presentation.channels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentChannelBinding
import com.example.anotheriptv.presentation.channels.Adapter.ChannelAdapter
import com.example.anotheriptv.presentation.channels.ViewModel.ChannelViewModel
import com.example.anotheriptv.presentation.channels.ViewModelFactory.ChannelViewModelFactory
import com.example.anotheriptv.presentation.player.PlayerActivity
import kotlinx.coroutines.launch
import kotlin.jvm.java

class ChannelFragment : Fragment() {

    private var _binding: FragmentChannelBinding? = null
    private val binding get() = _binding!!

    private lateinit var channelAdapter: ChannelAdapter
    private var selectedCategory = "View All"

    private val viewModel: ChannelViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        ChannelViewModelFactory(
            container.getChannelsUseCase,
            container.addWatchHistoryUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChannelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playlistId = arguments?.getLong("playlistId") ?: return
        val playlistName = arguments?.getString("playlistName") ?: "Channels"

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.ivSearch.setOnClickListener {
            val searchFragment = SearchChannelFragment()

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, searchFragment)
                .addToBackStack(null)
                .commit()
        }

        setupRecyclerView()
        observeViewModel()

        viewModel.loadChannels(playlistId)
    }

    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter { channel ->
            viewModel.addToHistory(
                channelId = channel.id,
                channelName = channel.name,
                channelLogo = channel.logo
            )

            val intent = android.content.Intent(requireContext(), PlayerActivity::class.java).apply {
                putExtra("channelName", channel.name)
                putExtra("streamUrl", channel.url)
            }
            startActivity(intent)
        }

        binding.recyclerChannels.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = channelAdapter
        }

    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.filteredChannels.collect { channels ->
                        channelAdapter.submitList(channels)
                        updateTitle(channels.size)
                    }
                }

                launch {
                    viewModel.categories.collect { categories ->
                        setupCategories(categories)
                    }
                }
            }
        }
    }

    private fun setupCategories(categories: List<String>) {
        binding.categoryContainer.removeAllViews()
        categories.forEach { category ->
            val chip = com.example.anotheriptv.databinding.ItemCategoryChipBinding.inflate(
                layoutInflater, binding.categoryContainer, false
            )
            chip.root.text = category
            updateChipStyle(chip.root, category == selectedCategory)
            chip.root.setOnClickListener {
                selectedCategory = category
                refreshChips()
                viewModel.filterByCategory(category)
            }
            binding.categoryContainer.addView(chip.root)
        }
    }

    private fun refreshChips() {
        val container = binding.categoryContainer
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? android.widget.TextView ?: continue
            updateChipStyle(chip, chip.text == selectedCategory)
        }
    }

    private fun updateChipStyle(chip: android.widget.TextView, isSelected: Boolean) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.chip_selected_text))
            chip.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_check), null, null, null
            )
            chip.compoundDrawablePadding = 6
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.chip_unselected_text))
            chip.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }

    private fun updateTitle(count: Int) {
        binding.tvTitle.text = "IPTV Channels ($count)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}