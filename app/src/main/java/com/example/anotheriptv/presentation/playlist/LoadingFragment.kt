package com.example.anotheriptv.presentation.playlist

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentLoadingBinding
import com.example.anotheriptv.presentation.playlist.UiState.LoadingUiState
import com.example.anotheriptv.presentation.playlist.ViewModel.PlaylistViewModel
import com.example.anotheriptv.presentation.playlist.ViewModelFactory.PlaylistViewModelFactory
import kotlinx.coroutines.launch

class LoadingFragment : Fragment() {

    private var _binding: FragmentLoadingBinding? = null
    private val binding get() = _binding!!

    private var currentProgress = 0
    private var targetProgress  = 0
    private var statusText      = ""
    private val handler = Handler(Looper.getMainLooper())

    private val viewModel: PlaylistViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        PlaylistViewModelFactory(
            container.getPlaylistsUseCase,
            container.addPlaylistUseCase,
            container.deletePlaylistUseCase,
            container.addXstreamUseCase
        )
    }

    private var outerVisible = true
    private val circleAnimRunnable = object : Runnable {
        override fun run() {
            if (currentProgress < 100) {
                animateCircles()
                handler.postDelayed(this, 800)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUI(0, "Connecting")
        handler.post(circleAnimRunnable)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadingState.collect { state ->
                    when (state) {
                        is LoadingUiState.Loading -> {
                            updateProgress(state.progress, state.statusText)
                        }
                        is LoadingUiState.Success,
                        is LoadingUiState.Error -> {
                            // Tự pop khi xong
                            parentFragmentManager.popBackStack()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateProgress(progress: Int, status: String) {
        targetProgress = progress
        statusText     = status
        smoothProgress()
    }

    private fun smoothProgress() {
        handler.post(object : Runnable {
            override fun run() {
                if (currentProgress < targetProgress) {
                    currentProgress++
                    updateUI(currentProgress, statusText)
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    private fun updateStep(progress: Int) {
        val step = when {
            progress < 20  -> 1
            progress < 40  -> 2
            progress < 60  -> 3
            progress < 80  -> 4
            else           -> 5
        }

        val (iconRes, statusText, dots) = when (step) {
            1    -> Triple(R.drawable.ic_wifi,     "Connecting",               listOf(true,  false, false, false, false))
            2    -> Triple(R.drawable.ic_category, "Preparing categories",     listOf(false, true,  false, false, false))
            3    -> Triple(R.drawable.ic_tv1,      "Loading live channels",    listOf(false, false, true,  false, false))
            4    -> Triple(R.drawable.ic_library,  "Opening movie library",    listOf(false, false, false, true,  false))
            else -> Triple(R.drawable.ic_tv2,      "Preparing series library", listOf(false, false, false, false, true))
        }

        binding.ivStepIcon.setImageResource(iconRes)
        binding.tvLoadingStatus.text = statusText  // ← override text theo step

        val dotViews = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4, binding.dot5)
        dotViews.forEachIndexed { index, view ->
            val isSelected = dots[index]
            view.setBackgroundResource(
                if (isSelected) R.drawable.dot_selected else R.drawable.dot_unselected
            )
            view.layoutParams = view.layoutParams.also {
                it.width = if (isSelected)
                    (24 * resources.displayMetrics.density).toInt()
                else
                    (8 * resources.displayMetrics.density).toInt()
            }
        }
    }

    private fun updateUI(progress: Int, status: String) {
        binding.progressBar.progress = progress
        binding.tvPercent.text        = "$progress%"
        binding.tvLoadingStatus.text  = status
        updateStep(progress)
    }


    private fun animateCircles() {
        val fadeIn  = AlphaAnimation(0f, 1f).apply { duration = 600 }
        val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 600 }
        if (outerVisible) {
            binding.viewOuterCircle.startAnimation(fadeOut)
            binding.viewInnerCircle.startAnimation(fadeIn)
        } else {
            binding.viewOuterCircle.startAnimation(fadeIn)
            binding.viewInnerCircle.startAnimation(fadeOut)
        }
        outerVisible = !outerVisible
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}