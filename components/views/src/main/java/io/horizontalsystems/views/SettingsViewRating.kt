package io.horizontalsystems.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_settings_rating.view.*
import kotlinx.android.synthetic.main.view_settings_value_text.view.*

class SettingsViewRating @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : SettingsViewBase(context, attrs, defStyleAttr) {

    fun showValue(text: String?) {
        settingsValueRight.text = text
        settingsValueRight.isVisible = text != null
    }

    fun setColor(color: Int) {
        settingsValueRight.setTextColor(color)
        arrowIcon.setColorFilter(color)
    }

    init {
        inflate(context, R.layout.view_settings_rating, this)

        val attributes = context.obtainStyledAttributes(attrs, R.styleable.SettingsViewRating)
        try {
            showIcon(attributes.getDrawable(R.styleable.SettingsViewRating_icon))
            showValue(attributes.getString(R.styleable.SettingsViewRating_value))

            setListPosition(attributes.getInt(R.styleable.SettingsViewRating_position, 0))
            setColor(attributes.getColor(R.styleable.SettingsViewRating_color, context.getColor(R.color.jacob)))
        } finally {
            attributes.recycle()
        }
    }
}
