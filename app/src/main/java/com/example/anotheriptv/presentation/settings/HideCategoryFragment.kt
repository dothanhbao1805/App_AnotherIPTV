package com.example.anotheriptv.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.data.local.entity.CategoryEntity
import com.example.anotheriptv.presentation.playlist.LoadingFragment
import com.example.anotheriptv.presentation.settings.Adapter.HideCategoryAdapter
import com.example.anotheriptv.presentation.xstream.ContainerXstreamActivity
import kotlinx.coroutines.launch

class HideCategoryFragment : Fragment() {

    private var playlistId: Long = -1L
    private var hasChanges = false  // ← theo dõi có thay đổi không
    private var playlistName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_hide_category, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = arguments?.getLong("playlistId", -1L) ?: -1L
        playlistName = arguments?.getString("playlistName") ?: ""

        val rvLive = view.findViewById<RecyclerView>(R.id.rvLiveCategories)
        val rvMovies = view.findViewById<RecyclerView>(R.id.rvMovieCategories)
        val rvSeries = view.findViewById<RecyclerView>(R.id.rvSeriesCategories)

        rvLive.layoutManager = LinearLayoutManager(requireContext())
        rvMovies.layoutManager = LinearLayoutManager(requireContext())
        rvSeries.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<View>(R.id.btnBack).setOnClickListener {
            handleBack()
        }

        // Handle system back button
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBack()
                }
            }
        )

        loadCategories(rvLive, rvMovies, rvSeries, view)
    }

    private fun handleBack() {
        if (hasChanges) {
            // Ẩn bottom nav
            (requireActivity() as? ContainerXstreamActivity)?.setBottomNavVisible(false)

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoadingFragment().apply {
                    arguments = Bundle().apply {
                        putLong("playlistId", playlistId)
                        putString("playlistName", playlistName)

                        // ĐỔI isRefresh THÀNH isApplyHide
                        putBoolean("isApplyHide", true)
                    }
                })
                .commit()
        } else {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun loadCategories(
        rvLive: RecyclerView,
        rvMovies: RecyclerView,
        rvSeries: RecyclerView,
        view: View
    ) {
        val repository = (requireActivity().application as MyApp).container.playlistRepository

        viewLifecycleOwner.lifecycleScope.launch {
            val liveCategories = repository.getCategoriesByType(playlistId, "LIVE")
            val movieCategories = repository.getCategoriesByType(playlistId, "MOVIE")
            val seriesCategories = repository.getCategoriesByType(playlistId, "SERIES")

            rvLive.adapter = HideCategoryAdapter(liveCategories) { category, isHidden ->
                hasChanges = true  // ← đánh dấu có thay đổi
                updateVisibility(repository, category, isHidden)
            }

            rvMovies.adapter = HideCategoryAdapter(movieCategories) { category, isHidden ->
                hasChanges = true
                updateVisibility(repository, category, isHidden)
            }

            rvSeries.adapter = HideCategoryAdapter(seriesCategories) { category, isHidden ->
                hasChanges = true
                updateVisibility(repository, category, isHidden)
            }

            setupSelectAll(view, R.id.tvSelectAllLive, R.id.tvDeselectAllLive, rvLive, liveCategories, repository)
            setupSelectAll(view, R.id.tvSelectAllMovies, R.id.tvDeselectAllMovies, rvMovies, movieCategories, repository)
            setupSelectAll(view, R.id.tvSelectAllSeries, R.id.tvDeselectAllSeries, rvSeries, seriesCategories, repository)
        }
    }

    private fun setupSelectAll(
        view: View,
        selectId: Int,
        deselectId: Int,
        rv: RecyclerView,
        categories: List<CategoryEntity>,
        repository: com.example.anotheriptv.domain.repository.PlaylistRepository
    ) {
        view.findViewById<TextView>(selectId).setOnClickListener {
            hasChanges = true
            val adapter = rv.adapter as? HideCategoryAdapter ?: return@setOnClickListener
            adapter.setAllChecked(true)

            // Dùng lifecycleScope thay vì viewLifecycleOwner
            lifecycleScope.launch {
                categories.forEach { repository.updateCategoryVisibility(playlistId, it.categoryId, it.contentType, false) }
            }
        }

        view.findViewById<TextView>(deselectId).setOnClickListener {
            hasChanges = true
            val adapter = rv.adapter as? HideCategoryAdapter ?: return@setOnClickListener
            adapter.setAllChecked(false)

            // Dùng lifecycleScope
            lifecycleScope.launch {
                categories.forEach { repository.updateCategoryVisibility(playlistId, it.categoryId, it.contentType, true) }
            }
        }
    }

    private fun updateVisibility(
        repository: com.example.anotheriptv.domain.repository.PlaylistRepository,
        category: CategoryEntity,
        isHidden: Boolean
    ) {
        // Dùng lifecycleScope
        lifecycleScope.launch {
            repository.updateCategoryVisibility(playlistId, category.categoryId, category.contentType, isHidden)
        }
    }

}