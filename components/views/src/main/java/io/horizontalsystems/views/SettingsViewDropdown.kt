package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible

class SettingsViewDropdown @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SettingsViewBase(context, attrs, defStyleAttr) {

    fun showDropdownValue(value: String?) {
        findViewById<TextView>(R.id.dropdownValue)?.apply {
            text = value
            isVisible = value != null
        }
    }

    fun showDropdownIcon(show: Boolean) {
        findViewById<ImageView>(R.id.dropdownIcon)?.isVisible = show
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!findViewById<TextView>(R.id.settingsSubtitle)?.text.isNullOrBlank()) {
            setAsDoubleLine()
        }
    }

    init {
        inflate(context, R.layout.view_settings_dropdown, this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SettingsViewDropdown)
        try {
            showTitle(attributes.getString(R.styleable.SettingsViewDropdown_title))
            showSubtitle(attributes.getString(R.styleable.SettingsViewDropdown_subtitle))
            showIcon(attributes.getDrawable(R.styleable.SettingsViewDropdown_icon))
            showDropdownValue(attributes.getString(R.styleable.SettingsViewDropdown_value))
            setListPosition(attributes.getInt(R.styleable.SettingsViewDropdown_position, 0))
        } finally {
            attributes.recycle()
        }
    }
}
