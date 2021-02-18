package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_settings_item.view.*
import kotlinx.android.synthetic.main.view_settings_value_text.view.*

class SettingsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : SettingsViewBase(context, attrs, defStyleAttr) {

    fun showValue(text: String?) {
        settingsValueRight.text = text
        settingsValueRight.isVisible = text != null
    }

    fun showAttention(show: Boolean) {
        attentionIcon.isVisible = show
    }

    private fun showArrow(visible: Boolean) {
        arrowIcon.isVisible = visible
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!settingsSubtitle.text.isNullOrBlank()) {
            setAsDoubleLine()
        }
    }

    init {
        inflate(context, R.layout.view_settings_item, this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SettingsView)
        try {
            showTitle(attributes.getString(R.styleable.SettingsView_title))
            showSubtitle(attributes.getString(R.styleable.SettingsView_subtitle))
            showIcon(attributes.getDrawable(R.styleable.SettingsView_icon))
            showValue(attributes.getString(R.styleable.SettingsView_value))
            showArrow(attributes.getBoolean(R.styleable.SettingsView_showArrow, true))
            setListPosition(attributes.getInt(R.styleable.SettingsView_position, 0))
        } finally {
            attributes.recycle()
        }
    }
}
