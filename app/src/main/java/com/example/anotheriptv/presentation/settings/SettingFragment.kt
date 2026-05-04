package com.example.anotheriptv.presentation.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.anotheriptv.MyApp
import com.example.anotheriptv.R
import com.example.anotheriptv.presentation.playlist.LoadingFragment
import com.example.anotheriptv.presentation.settings.ViewModel.RefreshState
import com.example.anotheriptv.presentation.settings.ViewModel.SettingsViewModel
import com.example.anotheriptv.presentation.settings.ViewModelFactory.SettingsViewModelFactory
import com.example.anotheriptv.presentation.xstream.ContainerXstreamActivity
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    // SharedPreferences key
    companion object {
        const val PREF_NAME = "app_settings"
        const val KEY_LANGUAGE = "language_code"
        const val KEY_THEME    = "theme_mode"
    }

    private val viewModel: SettingsViewModel by viewModels {
        val container = (requireActivity().application as MyApp).container
        SettingsViewModelFactory(container.playlistRepository)
    }
    private var loadingDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutLanguage = view.findViewById<LinearLayout>(R.id.layout_language)
        val tvSelectedLanguage = view.findViewById<TextView>(R.id.tv_selected_language)

        val playlistId = arguments?.getLong("playlistId", -1L) ?: -1L
        val playlistName = arguments?.getString("playlistName") ?: ""

        val layoutTheme     = view.findViewById<LinearLayout>(R.id.layout_theme)
        val tvSelectedTheme = view.findViewById<TextView>(R.id.tv_selected_theme)

        val prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val currentTheme = prefs.getInt(KEY_THEME, androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        tvSelectedTheme.text = getThemeDisplayName(currentTheme)

        val switchSpeed = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_speed)
        switchSpeed.isChecked = prefs.getBoolean("speed_up_long_press", true)

        switchSpeed.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("speed_up_long_press", isChecked).apply()
        }

        layoutTheme.setOnClickListener {
            showThemeDialog(tvSelectedTheme, tvSelectedTheme)
        }

        view.findViewById<TextView>(R.id.tv_playlist_name).text = playlistName

        val currentCode = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        tvSelectedLanguage.text = getDisplayName(currentCode)

        layoutLanguage.setOnClickListener {
            showLanguageDialog(tvSelectedLanguage, tvSelectedLanguage)
        }

        viewModel.loadPlaylist(playlistId)
        view.findViewById<LinearLayout>(R.id.layout_refresh_content).setOnClickListener {
            if (playlistId != -1L) viewModel.refreshPlaylist(playlistId)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.refreshState.collect { state ->
                when (state) {
                    is RefreshState.Idle -> Unit
                    is RefreshState.Loading -> showLoadingDialog()
                    is RefreshState.Success -> {
                        dismissLoadingDialog()
                        navigateAfterRefresh(state.playlistType, playlistId, playlistName)
                    }
                    is RefreshState.NavigateToLoading -> {
                        // Ẩn bottom nav
                        (requireActivity() as? ContainerXstreamActivity)?.setBottomNavVisible(false)

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, LoadingFragment().apply {
                                arguments = Bundle().apply {
                                    putLong("playlistId", state.playlistId)
                                    putBoolean("isRefresh", true)
                                }
                            })
                            .addToBackStack(null)
                            .commit()
                        viewModel.resetRefreshState()
                    }
                    is RefreshState.Error -> {
                        dismissLoadingDialog()
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        val switchBg = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_bg)
        switchBg.isChecked = prefs.getBoolean("continue_playing_bg", false)

        switchBg.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("continue_playing_bg", isChecked).apply()
        }

    }

    private fun showLanguageDialog(tvSelected: TextView, anchorView: View) {
        val displayNames = resources.getStringArray(R.array.language_display_names)
        val codes = resources.getStringArray(R.array.language_codes)

        val prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val listPopup = androidx.appcompat.widget.ListPopupWindow(requireContext())
        listPopup.anchorView = anchorView
        listPopup.setDropDownGravity(android.view.Gravity.END)
        listPopup.width = 350
        listPopup.isModal = true

        // Đẩy popup lên đè lên chính anchorView
        listPopup.verticalOffset = -anchorView.height

        listPopup.setAdapter(
            android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayNames)
        )

        listPopup.setOnItemClickListener { _, _, which, _ ->
            val selectedCode = codes[which]
            val selectedName = displayNames[which]
            prefs.edit().putString(KEY_LANGUAGE, selectedCode).apply()
            tvSelected.text = selectedName
            listPopup.dismiss()
            applyLanguage(selectedCode)
        }

        listPopup.show()
    }

    private fun applyLanguage(languageCode: String) {
        // Restart activity để áp dụng
        requireActivity().recreate()
    }

    private fun getDisplayName(code: String): String {
        val codes = resources.getStringArray(R.array.language_codes)
        val names = resources.getStringArray(R.array.language_display_names)
        val index = codes.indexOf(code).takeIf { it >= 0 } ?: 0
        return names[index]
    }

    private fun showThemeDialog(tvSelected: TextView, anchorView: View) {
        val themes = arrayOf("Default", "Light", "Dark")
        val modes = arrayOf(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
        )

        val prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val listPopup = androidx.appcompat.widget.ListPopupWindow(requireContext())
        listPopup.anchorView = anchorView
        listPopup.setDropDownGravity(android.view.Gravity.END)
        listPopup.width = 350
        listPopup.isModal = true

        // Đẩy popup lên đè lên chính anchorView
        listPopup.verticalOffset = -anchorView.height

        listPopup.setAdapter(
            android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, themes)
        )

        listPopup.setOnItemClickListener { _, _, which, _ ->
            val selectedMode = modes[which]
            prefs.edit().putInt(KEY_THEME, selectedMode).apply()
            tvSelected.text = themes[which]
            listPopup.dismiss()
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(selectedMode)
        }

        listPopup.show()
    }

    private fun getThemeDisplayName(mode: Int): String {
        return when (mode) {
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO  -> "Light"
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES -> "Dark"
            else -> "Default"
        }
    }

    private fun showLoadingDialog() {
        if (loadingDialog?.isShowing == true) return
        val dialogView = layoutInflater.inflate(R.layout.layout_loading_dialog, null)
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun navigateAfterRefresh(type: String, playlistId: Long, playlistName: String) {
        if (type == "M3U") {
            // Quay về History tab thay vì restart activity
            val historyFragment = com.example.anotheriptv.presentation.history.HistoryFragment().apply {
                arguments = Bundle().apply {
                    putLong("playlistId", playlistId)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, historyFragment)
                .commit()

            // Sync bottom nav selection
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottom_navigation
            ).selectedItemId = R.id.nav_history

            return
        }

        // XSTREAM → restart activity (giữ nguyên như cũ)
        val intent = Intent(requireContext(), ContainerXstreamActivity::class.java).apply {
            putExtra("playlistId", playlistId)
            putExtra("playlistName", playlistName)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissLoadingDialog()
    }

}