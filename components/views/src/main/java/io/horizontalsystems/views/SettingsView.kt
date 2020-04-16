package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import kotlinx.android.synthetic.main.view_settings_item.view.*
import kotlinx.android.synthetic.main.view_settings_value_text.view.*

class SettingsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : SettingsViewBase(context, attrs, defStyleAttr) {

    fun showValue(text: String?) {
        settingsValueRight.text = text
        settingsValueRight.showIf(text != null)
    }

    fun showAttention(show: Boolean) {
        attentionIcon.showIf(show)
    }

    init {
        inflate(context, R.layout.view_settings_item, this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SettingsView)
        try {
            showTitle(attributes.getString(R.styleable.SettingsView_title))
            showSubtitle(attributes.getString(R.styleable.SettingsView_subtitle))
            showIcon(attributes.getDrawable(R.styleable.SettingsView_icon))
            showValue(attributes.getString(R.styleable.SettingsView_value))
            showBottomBorder(attributes.getBoolean(R.styleable.SettingsView_bottomBorder, false))
        } finally {
            attributes.recycle()
        }
    }
}
