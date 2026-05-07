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
import com.example.anotheriptv.presentation.MainActivity
import com.example.anotheriptv.presentation.playlist.LoadingFragment
import com.example.anotheriptv.presentation.settings.ViewModel.RefreshState
import com.example.anotheriptv.presentation.settings.ViewModel.SettingsViewModel
import com.example.anotheriptv.presentation.settings.ViewModelFactory.SettingsViewModelFactory
import com.example.anotheriptv.presentation.xstream.ContainerXstreamActivity
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Configuration
import android.os.Build
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : Fragment() {

    // SharedPreferences key
    companion object {
        const val PREF_NAME = "app_settings"
        const val KEY_LANGUAGE = "language_code"
        const val KEY_THEME    = "theme_mode"
    }

    private var isPasswordVisible = false
    private var currentPassword = ""


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
        val playlistType = arguments?.getString("playlistType") ?: "M3U"

        val tvUserName = view.findViewById<TextView>(R.id.tv_user_name)
        val tvPassword = view.findViewById<TextView>(R.id.tv_password)
        val ivEyePassword = view.findViewById<ImageView>(R.id.iv_eye_password)
        val layoutPassword = view.findViewById<LinearLayout>(R.id.layout_password)
        val tvServerUrl = view.findViewById<TextView>(R.id.tv_server_url)

        setupVisibilityForType(playlistType, view)

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

        val switchDouble = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_double)
        switchDouble.isChecked = prefs.getBoolean("seek_double_tap", true)

        switchDouble.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("seek_double_tap", isChecked).apply()
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

        val tvServerUrlInfo = view.findViewById<TextView>(R.id.tv_server_info_url)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.playlist.collect { playlist ->
                if (playlist != null && playlist.type == "XSTREAM") {
                    // Đổ dữ liệu thật vào giao diện
                    tvUserName.text = playlist.userName ?: "N/A"
                    tvServerUrlInfo.text = playlist.url ?: "N/A"
                    currentPassword = playlist.password ?: ""
                    tvServerUrl.text = playlist.url ?: "N/A"

                    // Cập nhật giao diện Password lúc mới vào (thành dấu chấm)
                    updatePasswordUI(tvPassword, ivEyePassword)
                }
            }
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

        view.findViewById<LinearLayout>(R.id.layout_playlist_list).setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Copy Playlist Name
        view.findViewById<LinearLayout>(R.id.layout_playlist_name).setOnClickListener {
            val text = view.findViewById<TextView>(R.id.tv_playlist_name).text.toString()
            copyToClipboard(text)
        }

        // Copy Server URL
        view.findViewById<LinearLayout>(R.id.layout_server_url).setOnClickListener {
            val text = view.findViewById<TextView>(R.id.tv_server_url).text.toString()
            copyToClipboard(text)
        }

        // Copy User Name
        view.findViewById<LinearLayout>(R.id.layout_user_name).setOnClickListener {
            val text = view.findViewById<TextView>(R.id.tv_user_name).text.toString()
            if (text.isNotEmpty() && text != "N/A") {
                copyToClipboard(text)
            }
        }

        ivEyePassword.setOnClickListener {
            if (currentPassword.isNotEmpty()) {
                isPasswordVisible = !isPasswordVisible // Đảo ngược trạng thái
                updatePasswordUI(tvPassword, ivEyePassword)
            }
        }

        layoutPassword.setOnClickListener {
            if (currentPassword.isNotEmpty()) {
                // Sử dụng biến currentPassword để copy được chữ thật, thay vì copy ra các dấu chấm
                copyToClipboard(currentPassword)
            }
        }

        view.findViewById<LinearLayout>(R.id.hide_categories).setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HideCategoryFragment().apply {
                    arguments = Bundle().apply {
                        putLong("playlistId", playlistId)
                        putString("playlistName", playlistName)
                    }
                })
                .addToBackStack("hide_category")
                .commit()
        }

        view.findViewById<LinearLayout>(R.id.layout_subtitle).setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SubtitleSettingsFragment().apply {
                    arguments = Bundle().apply {
                        putLong("playlistId", playlistId)
                        putString("playlistName", playlistName)
                    }
                })
                .addToBackStack("layout_subtitle")
                .commit()
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
        // Lấy string đã dịch từ resources thay vì gõ cứng
        val themes = arrayOf(
            getString(R.string.theme_default),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )

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
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO  -> getString(R.string.theme_light)
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_default)
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

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("copied", text)
        clipboard.setPrimaryClip(clip)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            showCopiedSnackbar()
        }
    }

    private fun showCopiedSnackbar() {
        val snackbar = Snackbar.make(requireView(), "Copied to clipboard", Snackbar.LENGTH_SHORT)

        // Đặt phía trên bottom navigation
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        snackbar.anchorView = bottomNav

        // Custom màu theo light/dark
        val isDark = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        val bgColor = ContextCompat.getColor(requireContext(), R.color.snackbar_bg)
        val textColor = ContextCompat.getColor(requireContext(), R.color.snackbar_text)

        snackbar.view.backgroundTintList = ColorStateList.valueOf(bgColor)
        snackbar.setTextColor(textColor)

        // Bo góc
        snackbar.view.background = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 12f * resources.displayMetrics.density
        }

        snackbar.show()
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

    private fun setupVisibilityForType(type: String, view: View) {
        val isXstream = (type == "XSTREAM")

        // Nếu là XSTREAM thì VISIBLE (Hiện), ngược lại thì GONE (Ẩn hoàn toàn, không chiếm diện tích)
        val visibility = if (isXstream) View.VISIBLE else View.GONE

        // 1. Mục Connected
        view.findViewById<View>(R.id.layout_connected).visibility = visibility

        // 2. Mục Hide Categories & Đường gạch ngang
        view.findViewById<View>(R.id.hide_categories).visibility = visibility
        view.findViewById<View>(R.id.divider_hide_categories)?.visibility = visibility

        // 3. Mục Username & Password & Đường gạch ngang
        view.findViewById<View>(R.id.layout_user_name).visibility = visibility
        view.findViewById<View>(R.id.divider_user_name)?.visibility = visibility
        view.findViewById<View>(R.id.layout_password).visibility = visibility
        view.findViewById<View>(R.id.divider_password)?.visibility = visibility

        // 4. Mục Subscription Details
        view.findViewById<View>(R.id.tv_title_sub).visibility = visibility
        view.findViewById<View>(R.id.layout_sub_details).visibility = visibility

        // 5. Mục Server Information
        view.findViewById<View>(R.id.tv_title_server).visibility = visibility
        view.findViewById<View>(R.id.layout_server_info).visibility = visibility
    }

    private fun updatePasswordUI(tvPassword: TextView, ivEye: ImageView) {
        if (isPasswordVisible) {
            // Hiện mật khẩu thật, đổi icon thành mắt bị gạch (ic_eye_off)
            tvPassword.text = currentPassword
            ivEye.setImageResource(R.drawable.ic_eye_off)
        } else {
            // Ẩn mật khẩu, đổi thành dấu chấm tương ứng với độ dài mật khẩu thật
            val passLength = if (currentPassword.isNotEmpty()) currentPassword.length else 6
            tvPassword.text = "•".repeat(passLength)
            ivEye.setImageResource(R.drawable.ic_eye) // Icon mắt bình thường
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissLoadingDialog()
    }

}