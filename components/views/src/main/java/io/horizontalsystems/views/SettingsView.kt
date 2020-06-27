package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_settings_item.view.*
import kotlinx.android.synthetic.main.view_settings_left.view.*
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

    fun setTitleRed(){
        LayoutHelper.getAttr(R.attr.ColorLucian, context.theme)?.let { color ->
            settingsTitle.setTextColor(color)
        }
    }

    private fun showArrow(visible: Boolean) {
        arrowIcon.showIf(visible)
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
            showArrow(attributes.getBoolean(R.styleable.SettingsView_showArrow, true))
        } finally {
            attributes.recycle()
        }
    }
}
