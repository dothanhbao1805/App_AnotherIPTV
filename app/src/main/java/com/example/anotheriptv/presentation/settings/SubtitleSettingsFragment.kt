package com.example.anotheriptv.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.example.anotheriptv.R
import com.example.anotheriptv.databinding.FragmentSubtitleSettingsBinding

class SubtitleSettingsFragment : Fragment() {

    private var _binding: FragmentSubtitleSettingsBinding? = null
    private val binding get() = _binding!!

    // Default values
    private var fontSize      = 32f
    private var lineHeight    = 1.4f
    private var letterSpacing = 0.0f
    private var wordSpacing   = 0.0f
    private var padding       = 24f
    private var textColor: Int = android.graphics.Color.WHITE
    private var bgColor: Int   = android.graphics.Color.parseColor("#80000000")
    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density

    private var fontWeightIndex    = 1
    private var textAlignmentIndex = 1

    private lateinit var tooltipPopup: android.widget.PopupWindow
    private lateinit var tooltipText: android.widget.TextView
    // Default constants
    companion object {

        const val PREFS_NAME        = "subtitle_settings"
        const val KEY_FONT_SIZE     = "font_size"
        const val KEY_LINE_HEIGHT   = "line_height"
        const val KEY_LETTER_SPACING = "letter_spacing"
        const val KEY_WORD_SPACING  = "word_spacing"
        const val KEY_PADDING       = "padding"
        const val KEY_TEXT_COLOR    = "text_color"
        const val KEY_BG_COLOR      = "bg_color"
        const val KEY_FONT_WEIGHT     = "font_weight"
        const val KEY_TEXT_ALIGNMENT  = "text_alignment"

        // Font Size: 24.0 -> 96.0, step 4 → max = (96-24)/4 = 18
        const val FONT_SIZE_MIN  = 24f;  const val FONT_SIZE_STEP  = 4f;  const val FONT_SIZE_MAX_STEPS  = 18

        // Line Height: 1.0 -> 2.5, step 0.1 → max = (2.5-1.0)/0.1 = 15
        const val LINE_HEIGHT_MIN = 1.0f; const val LINE_HEIGHT_STEP = 0.1f; const val LINE_HEIGHT_MAX_STEPS = 15

        // Letter Spacing: -2.0 -> 5.0, step 0.1 → max = (5.0-(-2.0))/0.1 = 70
        const val LETTER_MIN = -2.0f;    const val LETTER_STEP = 0.1f;    const val LETTER_MAX_STEPS = 70

        // Word Spacing: -2.0 -> 10.0, step 0.1 → max = (10.0-(-2.0))/0.1 = 120
        const val WORD_MIN = -2.0f;      const val WORD_STEP = 0.1f;      const val WORD_MAX_STEPS = 120

        // Padding: 8.0 -> 48.0, step 1 → max = (48-8)/1 = 40
        const val PADDING_MIN = 8f;      const val PADDING_STEP = 1f;     const val PADDING_MAX_STEPS = 40

        fun newInstance() = SubtitleSettingsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubtitleSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSettings()

        setupTooltip()
        setupToolbar()
        setupSeekBars()
        updatePreview()
        setupColorPickers()
        setupSpinners()
        applyFontWeight()
        applyTextAlignment()

        setColorToView(binding.viewTextColor, textColor)
        setColorToView(binding.viewBgColor, bgColor)
        binding.tvSubtitlePreview.setTextColor(textColor)
        binding.tvSubtitlePreview.setBackgroundColor(bgColor)

        val maxPreviewHeight = (resources.displayMetrics.heightPixels * 0.55f).toInt()
        var isAdjustingHeight = false


        binding.framePreviewBox.addOnLayoutChangeListener { v, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (isAdjustingHeight) return@addOnLayoutChangeListener

            val newHeight = bottom - top
            val oldHeight = oldBottom - oldTop
            if (newHeight == oldHeight) return@addOnLayoutChangeListener  // không thay đổi → bỏ qua

            val params = v.layoutParams
            if (newHeight > maxPreviewHeight && params.height != maxPreviewHeight) {
                isAdjustingHeight = true
                params.height = maxPreviewHeight
                v.layoutParams = params
                v.post { isAdjustingHeight = false }
            } else if (newHeight <= maxPreviewHeight && params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                isAdjustingHeight = true
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                v.layoutParams = params
                v.post { isAdjustingHeight = false }
            }
        }

    }

    private fun saveSettings() {
        requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_FONT_SIZE,      fontSize)
            .putFloat(KEY_LINE_HEIGHT,    lineHeight)
            .putFloat(KEY_LETTER_SPACING, letterSpacing)
            .putFloat(KEY_WORD_SPACING,   wordSpacing)
            .putFloat(KEY_PADDING,        padding)
            .putInt(KEY_TEXT_COLOR,       textColor)
            .putInt(KEY_BG_COLOR,         bgColor)
            .putInt(KEY_FONT_WEIGHT,    fontWeightIndex)
            .putInt(KEY_TEXT_ALIGNMENT, textAlignmentIndex)
            .apply()
    }

    private fun loadSettings() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        fontSize      = prefs.getFloat(KEY_FONT_SIZE,      32f)
        lineHeight    = prefs.getFloat(KEY_LINE_HEIGHT,    1.4f)
        letterSpacing = prefs.getFloat(KEY_LETTER_SPACING, 0.0f)
        wordSpacing   = prefs.getFloat(KEY_WORD_SPACING,   0.0f)
        padding       = prefs.getFloat(KEY_PADDING,        24f)
        textColor     = prefs.getInt(KEY_TEXT_COLOR,       android.graphics.Color.WHITE)
        bgColor       = prefs.getInt(KEY_BG_COLOR,         android.graphics.Color.parseColor("#80000000"))
        fontWeightIndex    = prefs.getInt(KEY_FONT_WEIGHT,    1)
        textAlignmentIndex = prefs.getInt(KEY_TEXT_ALIGNMENT, 1)
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnReset.setOnClickListener {
            resetToDefaults()
        }
    }

    // ── SeekBars ──────────────────────────────────────────────────────────────

    private fun setupSeekBars() {
        // Font Size
        binding.seekBarFontSize.max = FONT_SIZE_MAX_STEPS
        binding.seekBarFontSize.progress = ((fontSize - FONT_SIZE_MIN) / FONT_SIZE_STEP).toInt()
        binding.tvFontSizeValue.text = formatOneDecimal(fontSize)
        binding.seekBarFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fontSize = FONT_SIZE_MIN + progress * FONT_SIZE_STEP
                binding.tvFontSizeValue.text = formatOneDecimal(fontSize)
                updatePreview()
                saveSettings()
                if (fromUser && seekBar != null) showTooltip(seekBar, formatOneDecimal(fontSize))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { hideTooltip() }
        })

        // Line Height
        binding.seekBarLineHeight.max = LINE_HEIGHT_MAX_STEPS
        binding.seekBarLineHeight.progress = ((lineHeight - LINE_HEIGHT_MIN) / LINE_HEIGHT_STEP).toInt()
        binding.tvLineHeightValue.text = formatOneDecimal(lineHeight)
        binding.seekBarLineHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lineHeight = LINE_HEIGHT_MIN + progress * LINE_HEIGHT_STEP
                binding.tvLineHeightValue.text = formatOneDecimal(lineHeight)
                updatePreview()
                saveSettings()
                if (fromUser && seekBar != null) showTooltip(seekBar, formatOneDecimal(lineHeight))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { hideTooltip() }
        })

        // Letter Spacing
        binding.seekBarLetterSpacing.max = LETTER_MAX_STEPS
        binding.seekBarLetterSpacing.progress = ((letterSpacing - LETTER_MIN) / LETTER_STEP).toInt()
        binding.tvLetterSpacingValue.text = formatOneDecimal(letterSpacing)
        binding.seekBarLetterSpacing.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                letterSpacing = LETTER_MIN + progress * LETTER_STEP
                binding.tvLetterSpacingValue.text = formatOneDecimal(letterSpacing)
                updatePreview()
                saveSettings()
                if (fromUser && seekBar != null) showTooltip(seekBar, formatOneDecimal(letterSpacing))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { hideTooltip() }
        })

        // Word Spacing
        binding.seekBarWordSpacing.max = WORD_MAX_STEPS
        binding.seekBarWordSpacing.progress = ((wordSpacing - WORD_MIN) / WORD_STEP).toInt()
        binding.tvWordSpacingValue.text = formatOneDecimal(wordSpacing)
        binding.seekBarWordSpacing.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                wordSpacing = WORD_MIN + progress * WORD_STEP
                binding.tvWordSpacingValue.text = formatOneDecimal(wordSpacing)
                updatePreview()
                saveSettings()
                if (fromUser && seekBar != null) showTooltip(seekBar, formatOneDecimal(wordSpacing))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { hideTooltip() }
        })

        // Padding
        binding.seekBarPadding.max = PADDING_MAX_STEPS
        binding.seekBarPadding.progress = ((padding - PADDING_MIN) / PADDING_STEP).toInt()
        binding.tvPaddingValue.text = formatOneDecimal(padding)
        binding.seekBarPadding.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                padding = PADDING_MIN + progress * PADDING_STEP
                binding.tvPaddingValue.text = formatOneDecimal(padding)
                updatePreview()
                saveSettings()
                if (fromUser && seekBar != null) showTooltip(seekBar, formatOneDecimal(padding))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) { hideTooltip() }
        })
    }

    private fun setupSpinners() {
        // Font Weight
        binding.spinnerFontWeight.setSelection(fontWeightIndex)
        binding.spinnerFontWeight.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                fontWeightIndex = position
                applyFontWeight()
                saveSettings()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Text Alignment
        binding.spinnerTextAlignment.setSelection(textAlignmentIndex)
        binding.spinnerTextAlignment.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                textAlignmentIndex = position
                applyTextAlignment()
                saveSettings()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun applyFontWeight() {
        binding.tvSubtitlePreview.typeface = when (fontWeightIndex) {
            0 -> android.graphics.Typeface.create("sans-serif-thin",   android.graphics.Typeface.NORMAL)
            1 -> android.graphics.Typeface.create("sans-serif",        android.graphics.Typeface.NORMAL)
            2 -> android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            3 -> android.graphics.Typeface.create("sans-serif",        android.graphics.Typeface.BOLD)
            4 -> android.graphics.Typeface.create("sans-serif",        android.graphics.Typeface.BOLD)
            else -> android.graphics.Typeface.DEFAULT
        }
        if (fontWeightIndex == 4) {
            // Extra Bold — dùng TextPaint fakeBoldText
            binding.tvSubtitlePreview.paintFlags =
                binding.tvSubtitlePreview.paintFlags or android.graphics.Paint.FAKE_BOLD_TEXT_FLAG
        } else {
            binding.tvSubtitlePreview.paintFlags =
                binding.tvSubtitlePreview.paintFlags and android.graphics.Paint.FAKE_BOLD_TEXT_FLAG.inv()
        }
    }

    private fun applyTextAlignment() {
        binding.tvSubtitlePreview.gravity = when (textAlignmentIndex) {
            0 -> android.view.Gravity.START   or android.view.Gravity.CENTER_VERTICAL
            1 -> android.view.Gravity.CENTER
            2 -> android.view.Gravity.END     or android.view.Gravity.CENTER_VERTICAL
            3 -> android.view.Gravity.START   or android.view.Gravity.CENTER_VERTICAL  // Justify fallback
            else -> android.view.Gravity.CENTER
        }
        // Justify thực sự (API 26+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            binding.tvSubtitlePreview.justificationMode = if (textAlignmentIndex == 3) {
                android.graphics.text.LineBreaker.JUSTIFICATION_MODE_INTER_WORD
            } else {
                android.graphics.text.LineBreaker.JUSTIFICATION_MODE_NONE
            }
        }
    }

    // ── Preview ───────────────────────────────────────────────────────────────

    private fun updatePreview() {
        binding.tvSubtitlePreview.apply {
            textSize      = this@SubtitleSettingsFragment.fontSize / 2f  // scale down 50% để preview gọn
            letterSpacing = this@SubtitleSettingsFragment.letterSpacing / 10f
            val p  = (this@SubtitleSettingsFragment.padding / 2f).toInt().dpToPx()
            val pV = (this@SubtitleSettingsFragment.padding / 4f).toInt().dpToPx()
            setPadding(p, pV, p, pV)
            setLineSpacing(0f, this@SubtitleSettingsFragment.lineHeight)
        }
    }


    // ── Reset ─────────────────────────────────────────────────────────────────

    private fun resetToDefaults() {
        fontSize      = 32f
        lineHeight    = 1.4f
        letterSpacing = 0.0f
        wordSpacing   = 0.0f
        padding       = 24f
        textColor     = android.graphics.Color.WHITE
        bgColor       = android.graphics.Color.parseColor("#80000000")

        binding.seekBarFontSize.progress      = ((fontSize - FONT_SIZE_MIN) / FONT_SIZE_STEP).toInt()
        binding.seekBarLineHeight.progress    = ((lineHeight - LINE_HEIGHT_MIN) / LINE_HEIGHT_STEP).toInt()
        binding.seekBarLetterSpacing.progress = ((letterSpacing - LETTER_MIN) / LETTER_STEP).toInt()
        binding.seekBarWordSpacing.progress   = ((wordSpacing - WORD_MIN) / WORD_STEP).toInt()
        binding.seekBarPadding.progress       = ((padding - PADDING_MIN) / PADDING_STEP).toInt()

        binding.tvFontSizeValue.text      = formatOneDecimal(fontSize)
        binding.tvLineHeightValue.text    = formatOneDecimal(lineHeight)
        binding.tvLetterSpacingValue.text = formatOneDecimal(letterSpacing)
        binding.tvWordSpacingValue.text   = formatOneDecimal(wordSpacing)
        binding.tvPaddingValue.text       = formatOneDecimal(padding)

        setColorToView(binding.viewTextColor, textColor)
        setColorToView(binding.viewBgColor, bgColor)
        binding.tvSubtitlePreview.setTextColor(textColor)
        binding.tvSubtitlePreview.setBackgroundColor(bgColor)

        fontWeightIndex    = 1
        textAlignmentIndex = 1
        binding.spinnerFontWeight.setSelection(1)
        binding.spinnerTextAlignment.setSelection(1)
        applyFontWeight()
        applyTextAlignment()

        // Force reset height về WRAP_CONTENT trước khi updatePreview
        binding.framePreviewBox.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        binding.framePreviewBox.requestLayout()

        updatePreview()
        saveSettings() // ← lưu lại giá trị default
    }

    private fun setupTooltip() {
        tooltipText = android.widget.TextView(requireContext()).apply {
            setTextColor(android.graphics.Color.WHITE)
            textSize = 12f
            setPadding(16, 8, 16, 8)
            setBackgroundResource(R.drawable.bg_tooltip)  // tạo drawable bên dưới
            gravity = android.view.Gravity.CENTER
        }

        tooltipPopup = android.widget.PopupWindow(
            tooltipText,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isOutsideTouchable = true
            isFocusable = false
            elevation = 8f
        }
    }

    private fun showTooltip(seekBar: SeekBar, value: String) {
        tooltipText.text = value

        // Measure tooltip để biết width
        tooltipText.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val tooltipWidth = tooltipText.measuredWidth

        // Tính vị trí thumb trên seekBar
        val seekWidth = seekBar.width - seekBar.paddingStart - seekBar.paddingEnd
        val thumbOffset = (seekBar.progress.toFloat() / seekBar.max * seekWidth).toInt()
        val xOffset = seekBar.paddingStart + thumbOffset - tooltipWidth / 2

        tooltipPopup.dismiss()
        // showAsDropDown hiển thị phía trên seekBar
        tooltipPopup.showAsDropDown(seekBar, xOffset, -seekBar.height - tooltipText.measuredHeight - 8)
    }

    private fun hideTooltip() {
        tooltipPopup.dismiss()
    }

    private fun setupColorPickers() {
        binding.layoutTextColor.setOnClickListener {
            showColorPickerDialog(textColor) { color ->
                textColor = color
                setColorToView(binding.viewTextColor, color)
                binding.tvSubtitlePreview.setTextColor(color)
            }
        }
        binding.layoutBgColor.setOnClickListener {
            showColorPickerDialog(bgColor) { color ->
                bgColor = color
                setColorToView(binding.viewBgColor, color)
                binding.tvSubtitlePreview.setBackgroundColor(color)
                saveSettings()
            }
        }
    }

    private fun showColorPickerDialog(currentColor: Int, onColorSelected: (Int) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val colorMap = mapOf(
            R.id.colorWhite  to android.graphics.Color.WHITE,
            R.id.colorBlack  to android.graphics.Color.BLACK,
            R.id.colorRed    to android.graphics.Color.parseColor("#E8392A"),
            R.id.colorGreen  to android.graphics.Color.parseColor("#3CB530"),
            R.id.colorBlue   to android.graphics.Color.parseColor("#3AABF0"),
            R.id.colorYellow to android.graphics.Color.parseColor("#F5D020"),
            R.id.colorOrange to android.graphics.Color.parseColor("#F5A623"),
            R.id.colorPurple to android.graphics.Color.parseColor("#9B59B6"),
            R.id.colorGray1  to android.graphics.Color.parseColor("#555555"),
            R.id.colorGray2  to android.graphics.Color.parseColor("#888888"),
            R.id.colorGray3  to android.graphics.Color.parseColor("#BBBBBB")
        )

        val strokeColor = requireContext().getColor(R.color.sub_primary)

        colorMap.forEach { (viewId, color) ->
            val swatch = dialogView.findViewById<View>(viewId)

            // Kotlin vẽ background hoàn toàn, không phụ thuộc XML
            swatch.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 10f.dpToPx()
                setColor(color)
                if (color == currentColor) {
                    setStroke(4.dpToPx(), strokeColor)
                }
            }

            swatch.setOnClickListener {
                onColorSelected(color)
                saveSettings()
                dialog.dismiss()
            }
        }

        dialogView.findViewById<android.widget.TextView>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setColorToView(view: View, color: Int) {
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 10f.dpToPx()
            setColor(color)
        }
        view.background = drawable
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private fun formatOneDecimal(value: Float): String = String.format("%.1f", value)

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}