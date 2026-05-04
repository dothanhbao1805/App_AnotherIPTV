package com.example.anotheriptv.presentation.xstream.series

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentSeriesXstreamAllBinding
import com.example.anotheriptv.domain.model.CategoryWithChannels
import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.presentation.xstream.series.Adapter.ItemSeriesXstreamAllAdapter
import com.example.anotheriptv.presentation.xstream.series.ViewModel.SeriesXstreamViewModel
import com.example.anotheriptv.presentation.xstream.series.ViewModelFactory.SeriesXstreamViewModelFactory
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class SeriesXstreamAllFragment : Fragment() {

    private var _binding: FragmentSeriesXstreamAllBinding? = null
    private val binding get() = _binding!!

    private var playlistId: Long = -1L
    private var initialCategoryId: String = ""
    private var initialCategoryName: String = ""

    private lateinit var allSeriesAdapter: ItemSeriesXstreamAllAdapter

    // Cache toàn bộ data
    private var allCategoriesWithChannels: List<CategoryWithChannels> = emptyList()
    private var selectedCategoryId: String = "ALL"  // "ALL" = tất cả
    private var searchQuery: String = ""

    private val viewModel: SeriesXstreamViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        SeriesXstreamViewModelFactory(
            channelRepository      = container.channelRepository,
            categoryDao            = container.categoryDao,
            addWatchHistoryUseCase = container.addWatchHistoryUseCase,
            channelDao             = container.channelDao,
            xstreamParser          = container.xstreamParser
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeriesXstreamAllBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId          = arguments?.getLong("playlistId") ?: -1L
        initialCategoryId   = arguments?.getString("categoryId") ?: ""
        initialCategoryName = arguments?.getString("categoryName") ?: ""

        // Mặc định luôn chọn All khi vào trang
        selectedCategoryId = "ALL"

        // Title theo category được truyền vào
        binding.tvTitleSeries.text = if (initialCategoryName.isNotEmpty()) initialCategoryName else "All Series"

        binding.toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        setupRecyclerView()
        setupSearch()
        observeViewModel()

        viewModel.loadAllSeries(playlistId)
    }

    private fun setupRecyclerView() {
        allSeriesAdapter = ItemSeriesXstreamAllAdapter { channel ->
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    SeriesDetailFragment.newInstance(channel, playlistId)
                )
                .addToBackStack(null)
                .commit()
        }

        binding.recyclerChannels.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = allSeriesAdapter
        }
    }

    private fun setupSearch() {
        binding.ivSearch.setOnClickListener {
            val isVisible = binding.layoutSearch.visibility == View.VISIBLE
            binding.layoutSearch.visibility = if (isVisible) View.GONE else View.VISIBLE
            if (!isVisible) binding.etSearch.requestFocus()
        }

        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                binding.ivClearSearch.visibility =
                    if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilter()
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categoriesWithChannels.collect { categories ->
                    allCategoriesWithChannels = categories
                    setupCategoryChips(categories)
                    applyFilter()
                }
            }
        }
    }

    private fun setupCategoryChips(categories: List<CategoryWithChannels>) {
        binding.chipGroupFilter.removeAllViews()

        // Lấy channels của category được truyền vào
        val channelsInCategory = if (initialCategoryId.isEmpty()) {
            categories.flatMap { it.channels }
        } else {
            categories.find { it.categoryId == initialCategoryId }?.channels ?: emptyList()
        }

        // Lấy danh sách genre unique từ channels
        val genres = channelsInCategory
            .mapNotNull { it.genre }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        // Chip "All"
        binding.chipGroupFilter.addView(createChip("All", "ALL"))

        // Chip cho từng genre
        genres.forEach { genre ->
            binding.chipGroupFilter.addView(createChip(genre, genre))
        }

        refreshChips()
    }

    private fun refreshChips() {
        for (i in 0 until binding.chipGroupFilter.childCount) {
            val chip = binding.chipGroupFilter.getChildAt(i) as? Chip ?: continue
            val isSelected = chip.tag == selectedCategoryId
            chip.isChecked = isSelected
        }
    }

    private fun createChip(label: String, categoryId: String): Chip {
        return Chip(requireContext()).apply {
            text = label
            tag  = categoryId
            isCheckable = true
            isCheckedIconVisible = true

            chipBackgroundColor = android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(
                    android.graphics.Color.parseColor("#E3F2FD"),
                    android.graphics.Color.WHITE
                )
            )

            setTextColor(
                android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(
                        android.graphics.Color.parseColor("#1565C0"),
                        android.graphics.Color.parseColor("#666666")
                    )
                )
            )

            checkedIconTint = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#1565C0")
            )

            chipStrokeWidth = 1.5f
            chipStrokeColor = android.content.res.ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(
                    android.graphics.Color.parseColor("#1565C0"),
                    android.graphics.Color.parseColor("#E0E0E0")
                )
            )

            // Dùng onClick thay vì onCheckedChange để tránh loop
            setOnClickListener {
                selectedCategoryId = categoryId
                refreshChips()
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        // Lấy channels của category được truyền vào
        val channelsInCategory = if (initialCategoryId.isEmpty()) {
            allCategoriesWithChannels.flatMap { it.channels }
        } else {
            allCategoriesWithChannels
                .find { it.categoryId == initialCategoryId }
                ?.channels ?: emptyList()
        }

        // Filter theo genre đang chọn
        val filtered = channelsInCategory.filter { channel ->
            (selectedCategoryId == "ALL" || channel.genre == selectedCategoryId) &&
                    (searchQuery.isEmpty() || channel.name.contains(searchQuery, ignoreCase = true))
        }

        binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        allSeriesAdapter.submitList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}