package com.example.anotheriptv.presentation.xstream.series

import android.content.Intent
import android.net.Uri
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
import com.bumptech.glide.Glide
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentSeriesDetailBinding
import com.example.anotheriptv.domain.model.Channel
import com.example.anotheriptv.presentation.xstream.series.Adapter.SeasonAdapter
import com.example.anotheriptv.presentation.xstream.series.ViewModel.SeriesXstreamViewModel
import com.example.anotheriptv.presentation.xstream.series.ViewModelFactory.SeriesXstreamViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SeriesDetailFragment : Fragment() {

    private var _binding: FragmentSeriesDetailBinding? = null
    private val binding get() = _binding!!
    private var isFavorite: Boolean = false

    private lateinit var seasonAdapter: SeasonAdapter

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

    private val channel: Channel by lazy {
        arguments?.getParcelable(ARG_CHANNEL)
            ?: error("SeriesDetailFragment requires ARG_CHANNEL")
    }

    private val playlistId: Long by lazy {
        arguments?.getLong(ARG_PLAYLIST_ID, -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSeriesDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.apply {
            statusBarColor = android.graphics.Color.TRANSPARENT
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        binding.btnBack.setOnApplyWindowInsetsListener { v, insets ->
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = insets.systemWindowInsetTop + 16.dpToPx()
            v.layoutParams = params
            insets
        }

        viewModel.clearSeriesData()
        
        bindHeader()
        loadFavoriteStatus()
        setupSeasonRecycler()
        observeSeasons()
        observeLoadingState()
        fetchSeasons()
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private fun bindHeader() {
        Glide.with(this)
            .load(channel.logo)
            .placeholder(R.drawable.ic_tv_placeholder)
            .error(R.drawable.ic_tv_placeholder)
            .into(binding.ivPoster)

        binding.tvTitle.text       = channel.name
        binding.tvGenre.text       = channel.genre.orEmpty()
        binding.tvDescription.text = channel.description.orEmpty()
        binding.tvReleaseDate.text = channel.releaseDate.orEmpty()
        binding.tvGenreDetail.text = channel.genre.orEmpty()
        binding.tvCast.text        = channel.cast.orEmpty()
        binding.tvEposode.text     = channel.episodeDuration?.let { "$it min" }.orEmpty()
        binding.tvCategory.text    = channel.category
        binding.tvSeries.text      = channel.seriesId?.toString().orEmpty()

        val rating = channel.rating ?: 0f
        if (rating > 0f) {
            binding.layoutRating.visibility = View.VISIBLE
            binding.ratingBar.rating        = rating / 2f
            binding.tvRating.text           = String.format("★ %.1f", rating)
        } else {
            binding.layoutRating.visibility = View.GONE
        }

        val trailerUrl = buildTrailerUrl(channel.trailerUrl)
        if (trailerUrl != null) {
            binding.cardTrailer.visibility = View.VISIBLE
            binding.cardTrailer.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl)))
            }
        } else {
            binding.cardTrailer.visibility = View.GONE
        }

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnFavorite.setOnClickListener {
            isFavorite = !isFavorite
            updateFavoriteIcon(isFavorite)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val container = (requireActivity().application as MyApp).container
                // Update isFavorite trong channels table
                container.channelDao.updateFavorite(channel.id, isFavorite)
            }
        }

    }

    private fun buildTrailerUrl(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return if (raw.startsWith("http")) raw
        else "https://www.youtube.com/watch?v=$raw"
    }

    // ── Seasons ───────────────────────────────────────────────────────────────

    private fun fetchSeasons() {
        val seriesId = channel.seriesId
        android.util.Log.d("SeriesDebug", "channel.seriesId = $seriesId")
        android.util.Log.d("SeriesDebug", "channel.name = ${channel.name}")
        android.util.Log.d("SeriesDebug", "playlistId = $playlistId")

        if (seriesId == null) {
            android.util.Log.e("SeriesDebug", "seriesId is NULL → cannot load seasons")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val container    = (requireActivity().application as MyApp).container
            val playlistData = container.playlistRepository.getPlaylistById(playlistId)

            android.util.Log.d("SeriesDebug", "playlist = ${playlistData?.name}, url = ${playlistData?.url}")

            if (playlistData?.url != null
                && playlistData.userName != null
                && playlistData.password != null
            ) {
                viewModel.fetchAndLoadSeasons(
                    playlistId = playlistId,
                    seriesId   = seriesId,
                    baseUrl    = playlistData.url,
                    username   = playlistData.userName,
                    password   = playlistData.password,
                    seriesName = channel.name
                )
            } else {
                android.util.Log.w("SeriesDebug", "No playlist credentials → fallback to DB")
                viewModel.loadSeasons(playlistId, seriesId)
            }
        }
    }

    private fun loadFavoriteStatus() {
        isFavorite = false
        updateFavoriteIcon(false)

        android.util.Log.d("FavoriteDebug", "channel.id = ${channel.id}, channel.name = ${channel.name}, channel.url = ${channel.url}")

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val container = (requireActivity().application as MyApp).container
            val channelFromDb = container.channelDao.getChannelById(channel.id)
            android.util.Log.d("FavoriteDebug", "channelFromDb = ${channelFromDb?.name}, isFavorite = ${channelFromDb?.isFavorite}")

            isFavorite = channelFromDb?.isFavorite ?: false
            withContext(Dispatchers.Main) {
                updateFavoriteIcon(isFavorite)
            }
        }
    }

    private fun setupSeasonRecycler() {
        seasonAdapter = SeasonAdapter { season ->
            val bottomSheet = EpisodesBottomSheet.newInstance(
                playlistId   = playlistId,
                seriesId     = channel.seriesId ?: return@SeasonAdapter,
                seasonNumber = season.seasonNumber,
                seasonName   = season.name
            )
            bottomSheet.show(childFragmentManager, "EpisodesBottomSheet")
        }

        binding.recyclerSeasons.apply {
            adapter       = seasonAdapter
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
        }
    }

    private fun observeSeasons() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.seasons.collect { seasons ->
                    seasonAdapter.submitList(seasons)

                    // Tự động load episodes của season đầu tiên
                    if (seasons.isNotEmpty()) {
                        channel.seriesId?.let { seriesId ->
                            viewModel.loadEpisodes(
                                playlistId   = playlistId,
                                seriesId     = seriesId,
                                seasonNumber = seasons.first().seasonNumber
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeLoadingState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoadingSeasons.collect { isLoading ->
                    if (isLoading) {
                        binding.layoutLoading.visibility = View.VISIBLE
                        binding.contentContainer.visibility = View.GONE
                    } else {
                        binding.layoutLoading.visibility = View.GONE
                        binding.contentContainer.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        if (isFavorite) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_filled)
            binding.btnFavorite.imageTintList = null
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
            binding.btnFavorite.imageTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.WHITE
            )
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.apply {
            statusBarColor = androidx.core.content.ContextCompat
                .getColor(requireContext(), R.color.bg_main)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        _binding = null
    }

    companion object {
        const val ARG_CHANNEL     = "arg_channel"
        const val ARG_PLAYLIST_ID = "arg_playlist_id"

        fun newInstance(channel: Channel, playlistId: Long) =
            SeriesDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CHANNEL, channel)
                    putLong(ARG_PLAYLIST_ID, playlistId)
                }
            }
    }

}