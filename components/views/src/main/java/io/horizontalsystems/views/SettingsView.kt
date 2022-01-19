package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.view.isVisible

class SettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SettingsViewBase(context, attrs, defStyleAttr) {

    fun showValue(value: String?) {
        findViewById<TextView>(R.id.settingsValueRight)?.apply {
            text = value
            isVisible = value != null
        }
    }

    fun showValueWithColor(value: String?, @ColorRes color: Int? = null) {
        findViewById<TextView>(R.id.settingsValueRight)?.apply {
            text = value
            isVisible = value != null
            color?.let { setTextColor(context.getColor(it)) }
        }
    }

    fun showAttention(show: Boolean) {
        findViewById<ImageView>(R.id.attentionIcon)?.isVisible = show
    }

    private fun showArrow(visible: Boolean) {
        findViewById<ImageView>(R.id.arrowIcon)?.isVisible = visible
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (!findViewById<TextView>(R.id.settingsSubtitle)?.text.isNullOrBlank()) {
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
