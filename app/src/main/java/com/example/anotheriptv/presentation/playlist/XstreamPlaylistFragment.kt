package com.example.anotheriptv.presentation.playlist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentXstreamPlaylistBinding
import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.presentation.ContainerPlaylistActivity
import com.example.anotheriptv.presentation.playlist.UiState.PlaylistUiState
import com.example.anotheriptv.presentation.playlist.ViewModel.PlaylistViewModel
import com.example.anotheriptv.presentation.playlist.ViewModelFactory.PlaylistViewModelFactory
import kotlinx.coroutines.launch

class XstreamPlaylistFragment : Fragment() {

    private var _binding: FragmentXstreamPlaylistBinding? = null
    private val binding get() = _binding!!

    private var loadingDialog: AlertDialog? = null

    private val viewModel: PlaylistViewModel by activityViewModels {
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
        _binding = FragmentXstreamPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupPasswordToggle()
        setupTextWatchers()
        observeUiState()

        binding.btnCreatePlaylist.setOnClickListener {
            handleCreateAction()
        }
    }

    // ─── Tạo Playlist ───────────────────────────────────────────────────────────

    private fun handleCreateAction() {
        val name     = binding.etPlaylistName.text.toString().trim()
        val url      = binding.etUrl.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()   // không trim password

        // Validate
        if (name.isEmpty() || url.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showError()
            return
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            binding.layoutError.visibility = View.VISIBLE
            binding.tvErrorTitle.setText(R.string.error_title_xstream)
//            binding.tvErrorDesc.setText(R.string.error_url_invalid)
            return
        }

        hideError()

        // Chuẩn hoá URL: bỏ dấu / cuối nếu có
        val baseUrl = url.trimEnd('/')

        val playlist = Playlist(
            name       = name,
            type       = "XSTREAM",
            sourceType = "URL",
            url     = url,
            userName   = username,
            password   = password,
            createdAt  = System.currentTimeMillis()
        )

        viewModel.addPlaylist(playlist)
    }

    // ─── Observe ────────────────────────────────────────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PlaylistUiState.Loading -> {
                            showLoading()
                            binding.btnCreatePlaylist.isEnabled = false
                        }

                        is PlaylistUiState.Success -> {
                            hideLoading()
                            state.playlist?.let { newPlaylist ->
                                val intent = android.content.Intent(
                                    requireContext(),
                                    ContainerPlaylistActivity::class.java
                                ).apply {
                                    putExtra("playlistId",   newPlaylist.id)
                                    putExtra("playlistName", newPlaylist.name)
                                }
                                startActivity(intent)
                                viewModel.resetState()
                                parentFragmentManager.popBackStack()
                            }
                        }

                        is PlaylistUiState.Error -> {
                            hideLoading()
                            showError()
                            binding.btnCreatePlaylist.isEnabled = true
                            viewModel.resetState()
                        }

                        is PlaylistUiState.Idle -> updateButtonState()
                    }
                }
            }
        }
    }

    // ─── UI helpers ─────────────────────────────────────────────────────────────

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                hideError()
                updateButtonState()
            }
        }
        binding.etPlaylistName.addTextChangedListener(watcher)
        binding.etUrl.addTextChangedListener(watcher)
        binding.etUsername.addTextChangedListener(watcher)
        binding.etPassword.addTextChangedListener(watcher)
    }

    private fun setupPasswordToggle() {
        var isVisible = false
        binding.ivShowPassword.setOnClickListener {
            isVisible = !isVisible
            binding.etPassword.inputType = if (isVisible) {
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            // Giữ cursor ở cuối
            binding.etPassword.setSelection(binding.etPassword.text?.length ?: 0)
//            binding.ivShowPassword.setImageResource(
//                if (isVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
//            )
        }
    }

    private fun updateButtonState() {
        val filled = binding.etPlaylistName.text.toString().isNotBlank()
                && binding.etUrl.text.toString().isNotBlank()
                && binding.etUsername.text.toString().isNotBlank()
                && binding.etPassword.text.toString().isNotEmpty()

        binding.btnCreatePlaylist.isEnabled = filled
        binding.btnCreatePlaylist.setBackgroundResource(
            if (filled) R.drawable.bg_button_primary else R.drawable.bg_button_disabled
        )

        val contentColor = ContextCompat.getColor(
            requireContext(),
            if (filled) R.color.white else R.color.tab_unselected
        )
        binding.tvCreateBtn.setTextColor(contentColor)
        binding.btnCreatePlaylist
            .findViewById<ImageView>(R.id.ivSaveIcon)
            ?.setColorFilter(contentColor)
    }

    private fun showError() {
        binding.layoutError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.layoutError.visibility = View.GONE
    }

    private fun showLoading() {
        if (loadingDialog == null) {
            val dialogView = layoutInflater.inflate(R.layout.layout_loading_dialog, null)
            loadingDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create()
            loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        loadingDialog?.show()
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog?.dismiss()
        loadingDialog = null
        _binding = null
    }
}