package com.example.anotheriptv.presentation.playlist

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentM3uPlaylistBinding
import com.example.anotheriptv.domain.model.Playlist
import com.example.anotheriptv.presentation.ContainerPlaylistActivity
import com.example.anotheriptv.presentation.playlist.UiState.PlaylistUiState
import com.example.anotheriptv.presentation.playlist.ViewModel.PlaylistViewModel
import com.example.anotheriptv.presentation.playlist.ViewModelFactory.PlaylistViewModelFactory
import kotlinx.coroutines.launch

class M3uPlaylistFragment : Fragment() {

    private var _binding: FragmentM3uPlaylistBinding? = null
    private val binding get() = _binding!!

    private var loadingDialog: AlertDialog? = null
    private var selectedFileUri: Uri? = null
    private var isUrlTabSelected = true

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            binding.tvSelectedFile.text = getFileName(it)
            binding.tvSelectedFile.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            updateButtonState()
        }
    }

    private val viewModel: PlaylistViewModel by activityViewModels {
        val container = (requireActivity().application as MyApp).container
        PlaylistViewModelFactory(
            container.getPlaylistsUseCase,
            container.addPlaylistUseCase,
            container.deletePlaylistUseCase,
            container.addXstreamUseCase
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentM3uPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        selectTab(isUrl = true)
        observeUiState()

        binding.tabUrl.setOnClickListener { selectTab(isUrl = true) }
        binding.tabFile.setOnClickListener { selectTab(isUrl = false) }

        binding.layoutFileInput.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                binding.layoutError.visibility = View.GONE
                binding.tvvalid.visibility = View.GONE
                updateButtonState()
            }
        }

        binding.etPlaylistName.addTextChangedListener(textWatcher)
        binding.etUrl.addTextChangedListener(textWatcher)

        binding.btnCreatePlaylist.setOnClickListener {
            handleCreateAction()
        }
    }

    private fun handleCreateAction() {
        val name = binding.etPlaylistName.text.toString().trim()

        if (name.isEmpty()) {
            binding.layoutError.visibility = View.VISIBLE
            return
        }

        val playlist = if (isUrlTabSelected) {
            // --- LOGIC TAB URL ---
            val url = binding.etUrl.text.toString().trim()

            if (url.isEmpty()) {
                binding.layoutError.visibility = View.VISIBLE
                return
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                binding.tvvalid.visibility = View.VISIBLE
                return
            }

            Playlist(
                name = name,
                type = "M3U",
                sourceType = "URL",
                m3uUrl = url,
                createdAt = System.currentTimeMillis()
            )
        } else {
            // --- LOGIC TAB FILE ---
            if (selectedFileUri == null) {
                binding.layoutError.visibility = View.VISIBLE
                return
            }

            Playlist(
                name = name,
                type = "M3U",
                sourceType = "FILE",
                filePath = selectedFileUri.toString(),
                createdAt = System.currentTimeMillis()
            )
        }

        binding.layoutError.visibility = View.GONE
        binding.tvvalid.visibility = View.GONE

        viewModel.addPlaylist(playlist)
    }

    private fun selectTab(isUrl: Boolean) {
        isUrlTabSelected = isUrl
        val colorSelected = ContextCompat.getColor(requireContext(), R.color.white)
        val colorUnselected = ContextCompat.getColor(requireContext(), R.color.tab_unselected)

        binding.tabUrl.setBackgroundResource(if (isUrl) R.drawable.bg_tab_selected else android.R.color.transparent)
        binding.tabFile.setBackgroundResource(if (isUrl) android.R.color.transparent else R.drawable.bg_tab_selected)

        binding.ivTabUrlIcon.setColorFilter(if (isUrl) colorSelected else colorUnselected)
        binding.ivTabFileIcon.setColorFilter(if (isUrl) colorUnselected else colorSelected)
        binding.tvTabUrl.setTextColor(if (isUrl) colorSelected else colorUnselected)
        binding.tvTabFile.setTextColor(if (isUrl) colorUnselected else colorSelected)

        binding.layoutUrlInput.visibility = if (isUrl) View.VISIBLE else View.GONE
        binding.layoutFileInput.visibility = if (isUrl) View.GONE else View.VISIBLE
        binding.labelSource.text = if (isUrl) "M3U URL" else "M3U File"

        updateButtonState()
    }
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
                                val intent = android.content.Intent(requireContext(), ContainerPlaylistActivity::class.java).apply {
                                    putExtra("playlistId", newPlaylist.id)
                                    putExtra("playlistName", newPlaylist.name)
                                }
                                startActivity(intent)

                                viewModel.resetState()
                                parentFragmentManager.popBackStack()
                            }
                        }

                        is PlaylistUiState.Error -> {
                            hideLoading()

                            binding.layoutError.visibility = View.VISIBLE
                            binding.tvvalid.visibility = View.GONE
                            binding.btnCreatePlaylist.isEnabled = true
                            viewModel.resetState()
                        }
                        is PlaylistUiState.Idle -> {
                            updateButtonState()
                        }
                    }
                }
            }
        }
    }

    private fun updateButtonState() {
        val name = binding.etPlaylistName.text.toString().trim()
        val url = binding.etUrl.text.toString().trim()

        val isEnabled = if (isUrlTabSelected) {
            name.isNotEmpty() && url.isNotEmpty()
        } else {
            name.isNotEmpty() && selectedFileUri != null
        }

        binding.btnCreatePlaylist.isEnabled = isEnabled

        binding.btnCreatePlaylist.setBackgroundResource(
            if (isEnabled) R.drawable.bg_button_primary else R.drawable.bg_button_disabled
        )

        val contentColor = ContextCompat.getColor(
            requireContext(),
            if (isEnabled) R.color.white else R.color.tab_unselected
        )

        binding.tvCreateBtn.setTextColor(contentColor)
        binding.btnCreatePlaylist.findViewById<android.widget.ImageView>(R.id.ivSaveIcon)?.setColorFilter(contentColor)
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result ?: "selected_file.m3u"
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